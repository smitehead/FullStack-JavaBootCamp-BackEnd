package com.javajava.project.service;

import com.javajava.project.dto.*;
import com.javajava.project.entity.*;
import com.javajava.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
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
    private final PointChargeRepository pointChargeRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final MemberRepository memberRepository;
    private final PortOneClient portOneClient;
    private final SseService sseService;

    // <카드 등록>

    @Override
    @Transactional
    public void registerBillingKey(Long memberNo, BillingKeyRegisterRequestDto dto) {
        // 이미 카드가 등록된 경우 기존 카드 삭제 후 새 카드로 교체
        billingKeyRepository.findByMemberNo(memberNo)
                .ifPresent(billingKeyRepository::delete);

        BillingKey billingKey = BillingKey.builder()
                .memberNo(memberNo)
                .customerUid(dto.getCustomerUid())
                .cardName(dto.getCardName())
                .cardNo(dto.getCardNo())
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
    @Transactional
    public ChargeResponseDto charge(Long memberNo, ChargeRequestDto dto) {

        // 1. 금액 유효성 검사
        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new IllegalArgumentException("충전 금액은 0원보다 커야 합니다.");
        }

        // 2. 등록된 카드(빌링키) 조회
        BillingKey billingKey = billingKeyRepository.findByMemberNo(memberNo)
                .orElseThrow(() -> new IllegalStateException("등록된 카드가 없습니다. 먼저 카드를 등록해주세요."));

        // 3. merchantUid 생성 — 결제 고유 식별자
        // 형식: charge_{memberNo}_{yyyyMMddHHmmssSSS}
        String merchantUid = "charge_" + memberNo + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        // 4. PENDING 상태로 선 INSERT (멱등성 처리)
        // 동시에 요청이 2개 들어오면 UNIQUE 제약으로 하나는 실패 → 중복 결제 방지
        PointCharge pendingCharge = PointCharge.builder()
                .memberNo(memberNo)
                .merchantUid(merchantUid)
                .chargeAmount(dto.getAmount())
                .pointAmount(dto.getAmount()) // 1원 = 1포인트
                .status("PENDING")
                .build();

        try {
            pointChargeRepository.save(pendingCharge);
        } catch (DataIntegrityViolationException e) {
            // merchantUid 중복 — 동시 요청 Race Condition 처리
            log.warn("[Charge] 중복 결제 요청 감지. memberNo={}, merchantUid={}", memberNo, merchantUid);
            throw new IllegalStateException("이미 처리 중인 결제 요청이 있습니다. 잠시 후 다시 시도해주세요.");
        }

        // 5. PortOne 액세스 토큰 발급
        String accessToken = portOneClient.getAccessToken();

        // 6. 빌링키 기반 자동결제 요청
        Map<?, ?> paymentResult;
        try {
            paymentResult = portOneClient.requestBillingCharge(
                    accessToken, billingKey.getCustomerUid(), merchantUid, dto.getAmount());
        } catch (Exception e) {
            // 결제 요청 자체가 실패한 경우
            pendingCharge.setStatus("FAILED");
            pointChargeRepository.save(pendingCharge);
            log.error("[Charge] 결제 요청 실패. memberNo={}, error={}", memberNo, e.getMessage());
            throw new IllegalStateException("결제 요청에 실패했습니다: " + e.getMessage());
        }

        // 7. 결제 결과에서 impUid 추출
        String impUid = (String) paymentResult.get("imp_uid");

        // 8. PortOne 서버에서 실제 결제 정보 조회 (서버 검증의 핵심)
        Map<?, ?> verified = portOneClient.getPayment(accessToken, impUid);
        Long paidAmount = ((Number) verified.get("amount")).longValue();
        String paidStatus = (String) verified.get("status");

        // 9. 금액 검증 + 상태 검증
        boolean isValid = dto.getAmount().equals(paidAmount) && "paid".equals(paidStatus);

        if (!isValid) {
            // 검증 실패 -> 즉시 결제 취소 (환불)
            // 이걸 안 하면 고객 카드에서 돈은 빠졌는데 포인트는 안 오르는 사고 발생
            String cancelReason = !dto.getAmount().equals(paidAmount)
                    ? "금액 불일치 (요청: " + dto.getAmount() + ", 실제: " + paidAmount + ")"
                    : "결제 상태 비정상: " + paidStatus;

            portOneClient.cancelPayment(accessToken, impUid, cancelReason);

            pendingCharge.setStatus("FAILED");
            pointChargeRepository.save(pendingCharge);
            log.error("[Charge] 검증 실패 후 취소 처리. memberNo={}, reason={}", memberNo, cancelReason);
            throw new IllegalStateException("결제 검증에 실패했습니다. 결제가 취소되었습니다.");
        }

        // 10. 검증 성공 — 포인트 증가 + 이력 저장
        // @Transactional 범위 안이므로 하나라도 실패하면 전체 롤백
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        long newBalance = member.getPoints() + dto.getAmount();
        member.setPoints(newBalance);

        // 카드사 정보 업데이트
        pendingCharge.setPgTid(impUid);
        pendingCharge.setCardCompany((String) verified.get("card_name"));
        pendingCharge.setStatus("SUCCESS");
        pointChargeRepository.save(pendingCharge);

        // PointHistory에 이력 기록
        PointHistory history = PointHistory.builder()
                .memberNo(memberNo)
                .type("충전")
                .amount(dto.getAmount())
                .balance(newBalance)
                .reason("카드 충전 (" + pendingCharge.getCardCompany() + ")")
                .build();
        pointHistoryRepository.save(history);

        // 11. SSE로 실시간 포인트 갱신 이벤트 전송
        // 헤더의 포인트 표시가 새로고침 없이 즉시 반영됨
        try {
            sseService.sendPointUpdate(memberNo, newBalance);
        } catch (Exception e) {
            // SSE 실패는 결제 자체에 영향 주면 안 됨 — 롤백하지 않음
            log.warn("[Charge] SSE 포인트 갱신 실패 (결제는 성공). memberNo={}", memberNo);
        }

        log.info("[Charge] 충전 완료. memberNo={}, amount={}, newBalance={}",
                memberNo, dto.getAmount(), newBalance);

        return ChargeResponseDto.builder()
                .success(true)
                .newBalance(newBalance)
                .message(dto.getAmount() + "원이 충전되었습니다.")
                .build();
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