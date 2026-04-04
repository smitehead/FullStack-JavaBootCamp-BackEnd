package com.javajava.project.domain.point.service;

import com.javajava.project.domain.point.dto.BillingKeyRegisterRequestDto;
import com.javajava.project.domain.point.dto.BillingKeyResponseDto;
import com.javajava.project.domain.point.dto.ChargeRequestDto;
import com.javajava.project.domain.point.dto.ChargeResponseDto;
import com.javajava.project.domain.point.dto.PointHistoryResponseDto;
import com.javajava.project.domain.point.entity.BillingKey;
import com.javajava.project.domain.point.repository.BillingKeyRepository;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final BillingKeyRepository billingKeyRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PortOneClient portOneClient;
    private final PointTransactionHelper txHelper;

    // <카드 등록>

    @Override
    @Transactional
    public void registerBillingKey(Long memberNo, BillingKeyRegisterRequestDto dto) {

        // customer_uid: 회원당 고유 식별자 (고정값 사용 — 재등록 시 PortOne이 덮어씀)
        String customerUid = "customer_" + memberNo;

        // 1. PortOne 액세스 토큰 발급
        String accessToken = portOneClient.getAccessToken();

        // 2. PortOne API로 빌링키 발급 요청
        // 카드 정보는 PortOne 서버로만 전송되고 우리 DB에는 저장하지 않음 (PCI DSS 준수)
        Map<?, ?> result = portOneClient.issueBillingKey(
                accessToken,
                customerUid,
                dto.getCardNumber(),
                dto.getExpiry(),
                dto.getBirth(),
                dto.getPwd2digit());

        // 3. 응답에서 카드 정보 추출 (마스킹된 값만 반환됨)
        String cardName = (String) result.get("card_name"); // 예: 신한카드
        String cardNo = (String) result.get("card_number"); // 예: 4000-00**-****-0001

        // 4. 기존 카드 있으면 삭제 후 새 카드로 교체
        billingKeyRepository.findByMemberNo(memberNo)
                .ifPresent(billingKeyRepository::delete);

        BillingKey billingKey = BillingKey.builder()
                .memberNo(memberNo)
                .customerUid(customerUid)
                .cardName(cardName)
                .cardNo(cardNo)
                .build();

        billingKeyRepository.save(billingKey);
    }

    @Override
    @Transactional(readOnly = true)
    public BillingKeyResponseDto getBillingKey(Long memberNo) {
        return billingKeyRepository.findByMemberNo(memberNo)
                .map(BillingKeyResponseDto::from)
                .orElse(BillingKeyResponseDto.notRegistered());
    }

    @Override
    @Transactional
    public void deleteBillingKey(Long memberNo) {
        BillingKey billingKey = billingKeyRepository.findByMemberNo(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("등록된 카드가 없습니다."));
        billingKeyRepository.delete(billingKey);
    }

    // <포인트 충전>

    @Override
    public ChargeResponseDto charge(Long memberNo, ChargeRequestDto dto) {

        if (dto.getAmount() == null || dto.getAmount() < 1000) {
            throw new IllegalArgumentException("최소 충전 금액은 1,000원입니다.");
        }

        BillingKey billingKey = billingKeyRepository.findByMemberNo(memberNo)
                .orElseThrow(() -> new IllegalStateException("등록된 카드가 없습니다."));

        String merchantUid = "charge_" + memberNo + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        // STEP 1: PENDING INSERT (별도 빈에서 호출 → AOP 프록시 정상 작동)
        txHelper.insertPending(memberNo, merchantUid, dto.getAmount());

        // STEP 2: PortOne API 호출 (트랜잭션 없음)
        String accessToken;
        Map<?, ?> paymentResult;
        String impUid;

        try {
            accessToken = portOneClient.getAccessToken();
            paymentResult = portOneClient.requestBillingCharge(
                    accessToken, billingKey.getCustomerUid(), merchantUid, dto.getAmount());

            impUid = (String) paymentResult.get("imp_uid");
            log.info("[Charge] 결제 성공. imp_uid={}", impUid);

        } catch (Exception e) {
            txHelper.updateStatus(merchantUid, "FAILED", null, null);
            log.error("[Charge] 결제 요청 실패. memberNo={}, error={}", memberNo, e.getMessage());
            throw new IllegalStateException("결제 요청에 실패했습니다: " + e.getMessage());
        }

        // STEP 3: 응답 데이터로 검증 (별도 조회 없이 응답 직접 사용)
        Long paidAmount;
        String paidStatus;
        String cardCompany;

        try {
            paidAmount = ((Number) paymentResult.get("amount")).longValue();
            paidStatus = (String) paymentResult.get("status");
            cardCompany = (String) paymentResult.get("card_name");

            log.info("[Charge] 검증 - amount={}, status={}, card={}",
                    paidAmount, paidStatus, cardCompany);

        } catch (Exception e) {
            log.error("[Charge] 검증 데이터 파싱 실패. merchantUid={}", merchantUid);
            throw new IllegalStateException("결제 검증 중 오류가 발생했습니다. 잠시 후 포인트가 자동 지급됩니다.");
        }

        // 금액 검증
        if (!dto.getAmount().equals(paidAmount) || !"paid".equals(paidStatus)) {
            portOneClient.cancelPayment(accessToken, impUid, "금액 불일치 또는 상태 비정상");
            txHelper.updateStatus(merchantUid, "FAILED", null, null);
            throw new IllegalStateException("결제 검증에 실패했습니다. 결제가 취소되었습니다.");
        }

        // STEP 4: 포인트 지급 (별도 빈에서 호출 → AOP 프록시 정상 작동)
        return txHelper.completeCharge(memberNo, merchantUid, impUid, cardCompany, dto.getAmount());
    }

    // <포인트 내역 조회>

    @Override
    @Transactional(readOnly = true)
    public Page<PointHistoryResponseDto> getHistory(Long memberNo, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return pointHistoryRepository
                .findByMemberNoOrderByCreatedAtDesc(memberNo, pageable)
                .map(PointHistoryResponseDto::from);
    }
}