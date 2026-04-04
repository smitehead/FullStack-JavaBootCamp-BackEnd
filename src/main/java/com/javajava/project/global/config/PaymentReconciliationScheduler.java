package com.javajava.project.global.config;

import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.point.entity.PointCharge;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.point.repository.PointChargeRepository;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.point.service.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PointChargeRepository pointChargeRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PortOneClient portOneClient;

    /**
     * 매 5분마다 10분 이상 PENDING 상태인 결제 보정
     * 돈은 빠졌는데 포인트가 안 오른 "고스트 결제" 처리
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void reconcilePendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<PointCharge> pendingList = pointChargeRepository
                .findByStatusAndChargedAtBefore("PENDING", cutoff);

        if (pendingList.isEmpty())
            return;

        log.info("[Reconciliation] PENDING 결제 {} 건 보정 시작", pendingList.size());

        String accessToken = portOneClient.getAccessToken();

        for (PointCharge charge : pendingList) {
            try {
                // merchantUid로 PortOne 결제 조회
                Map<?, ?> payment = portOneClient.getPaymentByMerchantUid(
                        accessToken, charge.getMerchantUid());

                if (payment == null) {
                    // PortOne에 결제 기록 자체가 없으면 FAILED 처리
                    charge.setStatus("FAILED");
                    log.warn("[Reconciliation] 결제 기록 없음 → FAILED. merchantUid={}",
                            charge.getMerchantUid());
                    continue;
                }

                String paidStatus = (String) payment.get("status");
                Long paidAmount = ((Number) payment.get("amount")).longValue();

                if ("paid".equals(paidStatus) && charge.getChargeAmount().equals(paidAmount)) {

                    // 이미 SUCCESS인 건 스킵 (completeCharge와 동시 실행 방지)
                    if ("SUCCESS".equals(charge.getStatus())) {
                        log.info("[Reconciliation] 이미 SUCCESS 처리됨. 스킵. merchantUid={}",
                                charge.getMerchantUid());
                        continue;
                    }
                    // 실제로 결제 성공 → 포인트 지급
                    Member member = memberRepository.findByIdWithLock(charge.getMemberNo())
                            .orElseThrow();

                    long newBalance = member.getPoints() + charge.getChargeAmount();
                    member.setPoints(newBalance);

                    charge.setStatus("SUCCESS");
                    charge.setPgTid((String) payment.get("imp_uid"));

                    pointHistoryRepository.save(PointHistory.builder()
                            .memberNo(charge.getMemberNo())
                            .type("충전")
                            .amount(charge.getChargeAmount())
                            .balance(newBalance)
                            .reason("카드 충전 (보정 처리)")
                            .build());

                    log.info("[Reconciliation] 보정 완료 → SUCCESS. merchantUid={}, amount={}",
                            charge.getMerchantUid(), charge.getChargeAmount());
                } else {
                    charge.setStatus("FAILED");
                    log.warn("[Reconciliation] 결제 상태 비정상 → FAILED. status={}, merchantUid={}",
                            paidStatus, charge.getMerchantUid());
                }
            } catch (Exception e) {
                log.error("[Reconciliation] 보정 처리 중 오류. merchantUid={}, error={}",
                        charge.getMerchantUid(), e.getMessage());
            }
        }
    }
}