package com.javajava.project.domain.auction.scheduler;

import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 7일 자동 구매 확정 스케줄러.
 *
 * <h3>정책 (기존 12시간 자동 취소 대체)</h3>
 * <ul>
 *   <li>낙찰 후 5일 경과(D-2): 구매자에게 자동 확정 2일 전 사전 알림</li>
 *   <li>낙찰 후 6일 경과(D-1): 구매자에게 자동 확정 1일 전 사전 알림</li>
 *   <li>낙찰 후 7일 경과(D-0): 자동 구매 확정 + 에스크로 정산 실행</li>
 * </ul>
 *
 * <h3>Self-Invocation 방지</h3>
 * <p>실제 트랜잭션 처리는 {@link AuctionAutoConfirmProcessor}(외부 빈)에 위임.
 * 이 클래스는 오직 "대상 조회 + 위임" 만 담당하며 {@code @Transactional} 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionAutoConfirmScheduler {

    private final AuctionResultRepository auctionResultRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final AuctionAutoConfirmProcessor processor;

    // ──────────────────────────────────────────────────────────────────────────
    // 매시간 정각: 낙찰 후 7일 경과 건 자동 구매 확정
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 매시간 정각 실행 — 7일 경과 자동 구매 확정.
     *
     * <p>{@code paymentDueDate <= now}인 '배송대기' 건을 일괄 수집하여
     * 단건별 독립 트랜잭션({@code REQUIRES_NEW})으로 처리.
     * 개별 건 실패가 다른 건에 영향을 주지 않는다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkAutoConfirm() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionResult> targets = auctionResultRepository.findExpiredPendingPayments(now);

        if (targets.isEmpty()) return;

        log.info("[AutoConfirmScheduler] 7일 자동 구매 확정 대상 {}건 탐지", targets.size());

        for (AuctionResult target : targets) {
            try {
                processor.autoConfirm(target.getResultNo());
            } catch (Exception e) {
                log.error("[AutoConfirmScheduler] resultNo={} 자동 확정 오류: {}",
                        target.getResultNo(), e.getMessage(), e);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 매일 오전 10시: D-2, D-1 사전 알림 발송
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 매일 오전 10시 실행 — 자동 확정 D-2, D-1 사전 알림.
     *
     * <p>일별로 한 번만 실행되므로 알림 중복 발송을 방지할 수 있다.
     * (24시간 단위 window로 구간을 나눠 각 건이 정확히 한 번 알림을 받음)
     *
     * <ul>
     *   <li>D-1 구간: paymentDueDate 가 now ~ now+24h 사이 → 내일 자동 확정</li>
     *   <li>D-2 구간: paymentDueDate 가 now+24h ~ now+48h 사이 → 모레 자동 확정</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendAutoConfirmReminders() {
        LocalDateTime now = LocalDateTime.now();

        // D-1: 자동 확정 1일 전 (내일 확정 예정)
        List<AuctionResult> d1Targets = auctionResultRepository.findPendingInWindow(now, now.plusDays(1));
        for (AuctionResult result : d1Targets) {
            sendReminderNotification(result, 1);
        }

        // D-2: 자동 확정 2일 전 (모레 확정 예정)
        List<AuctionResult> d2Targets = auctionResultRepository.findPendingInWindow(now.plusDays(1), now.plusDays(2));
        for (AuctionResult result : d2Targets) {
            sendReminderNotification(result, 2);
        }

        log.info("[AutoConfirmScheduler] D-1 알림 {}건, D-2 알림 {}건 발송 완료",
                d1Targets.size(), d2Targets.size());
    }

    // ── private 헬퍼 ─────────────────────────────────────────────────────────

    private void sendReminderNotification(AuctionResult result, int daysLeft) {
        try {
            BidHistory bid = bidHistoryRepository.findById(result.getBidNo()).orElse(null);
            if (bid == null) {
                log.warn("[AutoConfirmScheduler] D-{} 알림 건너뜀 — bid 없음 (resultNo={})",
                        daysLeft, result.getResultNo());
                return;
            }

            Product product = productRepository.findById(bid.getProductNo()).orElse(null);
            if (product == null) {
                log.warn("[AutoConfirmScheduler] D-{} 알림 건너뜀 — product 없음 (resultNo={})",
                        daysLeft, result.getResultNo());
                return;
            }

            notificationService.sendAndSaveNotification(
                    bid.getMemberNo(), "activity",
                    "낙찰받으신 [" + product.getTitle() + "]의 구매 확정이 " + daysLeft
                            + "일 후 자동으로 진행됩니다. 상품을 수령하셨다면 구매 확정을 눌러주세요.",
                    "/won/" + product.getProductNo());

        } catch (Exception e) {
            log.warn("[AutoConfirmScheduler] D-{} 알림 실패 (resultNo={}): {}",
                    daysLeft, result.getResultNo(), e.getMessage());
        }
    }
}
