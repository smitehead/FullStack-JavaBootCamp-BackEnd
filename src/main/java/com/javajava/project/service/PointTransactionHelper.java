package com.javajava.project.service;

import com.javajava.project.dto.ChargeResponseDto;
import com.javajava.project.entity.Member;
import com.javajava.project.entity.PointCharge;
import com.javajava.project.entity.PointHistory;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.repository.PointChargeRepository;
import com.javajava.project.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointTransactionHelper {

    private final PointChargeRepository pointChargeRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SseService sseService;

    /**
     * STEP 1: PENDING INSERT — 별도 트랜잭션으로 즉시 커밋
     * 이후 로직이 실패해도 PENDING 기록은 남아 스케줄러가 보정 가능
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertPending(Long memberNo, String merchantUid, Long amount) {
        try {
            pointChargeRepository.save(PointCharge.builder()
                    .memberNo(memberNo)
                    .merchantUid(merchantUid)
                    .chargeAmount(amount)
                    .pointAmount(amount)
                    .discount(0L)
                    .status("PENDING")
                    .build());
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("MERCHANT_UID") || msg.contains("UQ_MERCHANT")) {
                throw new IllegalStateException("이미 처리 중인 결제 요청입니다.");
            }
            throw new IllegalStateException("결제 요청 저장 실패: " + msg);
        }
    }

    /**
     * STEP 2: 상태 업데이트 — 별도 트랜잭션
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(String merchantUid, String status,
            String impUid, String cardCompany) {
        pointChargeRepository.findByMerchantUid(merchantUid).ifPresent(charge -> {
            charge.setStatus(status);
            if (impUid != null)
                charge.setPgTid(impUid);
            if (cardCompany != null)
                charge.setCardCompany(cardCompany);
        });
    }

    /**
     * STEP 3: 검증 성공 시 포인트 지급 — 별도 트랜잭션 + 비관적 락
     * SELECT FOR UPDATE로 Lost Update 방지
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChargeResponseDto completeCharge(Long memberNo, String merchantUid,
            String impUid, String cardCompany, Long amount) {

        // 이미 SUCCESS 처리된 건인지 먼저 확인 (중복 지급 방지)
        PointCharge charge = pointChargeRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalStateException("결제 기록을 찾을 수 없습니다."));

        if ("SUCCESS".equals(charge.getStatus())) {
            log.warn("[Charge] 이미 처리된 결제입니다. merchantUid={}", merchantUid);
            Member member = memberRepository.findById(memberNo)
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
            return ChargeResponseDto.builder()
                    .success(true)
                    .newBalance(member.getPoints())
                    .message("이미 처리된 충전입니다.")
                    .build();
        }

        // 비관적 락으로 Member 조회
        Member member = memberRepository.findByIdWithLock(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        long newBalance = member.getPoints() + amount;
        member.setPoints(newBalance);

        // SUCCESS 업데이트 — 같은 트랜잭션에서 처리
        charge.setStatus("SUCCESS");
        if (impUid != null)
            charge.setPgTid(impUid);
        if (cardCompany != null)
            charge.setCardCompany(cardCompany);

        // 포인트 이력 저장
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(memberNo)
                .type("충전")
                .amount(amount)
                .balance(newBalance)
                .reason("카드 충전" + (cardCompany != null ? " (" + cardCompany + ")" : ""))
                .build());

        // SSE (실패해도 무관)
        try {
            sseService.sendPointUpdate(memberNo, newBalance);
        } catch (Exception e) {
            log.warn("[Charge] SSE 갱신 실패. memberNo={}", memberNo);
        }

        log.info("[Charge] 충전 완료. memberNo={}, amount={}, newBalance={}",
                memberNo, amount, newBalance);

        return ChargeResponseDto.builder()
                .success(true)
                .newBalance(newBalance)
                .message(amount + "원이 충전되었습니다.")
                .build();
    }
}