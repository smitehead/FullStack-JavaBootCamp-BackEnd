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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 만료 검증 배치 스케줄러 (Phase 2 & Phase 3).
 *
 * <h3>Phase 2 — 차순위 승계</h3>
 * <ul>
 *   <li>10분 주기로 status=3 (PENDING_PAYMENT) 상품을 조회</li>
 *   <li>paymentDueDate 가 지난 AuctionResult 탐지</li>
 *   <li>현재 낙찰자 bid를 isCancelled=1 처리 후 다음 입찰자에게 12시간 부여</li>
 * </ul>
 *
 * <h3>Phase 3 — 최종 유찰</h3>
 * <ul>
 *   <li>대기 입찰자가 없거나 모두 결제 불이행 시 status=4 (CLOSED_FAILED)</li>
 *   <li>판매자에게 최종 유찰 알림 발송</li>
 * </ul>
 *
 * <p><b>트랜잭션 전략:</b>
 * 목록 조회(Scheduler)와 상품별 처리(processPaymentExpiry)를 분리하여,
 * 개별 상품 처리 실패가 다른 상품에 영향을 주지 않도록 독립 트랜잭션으로 실행.
 *
 * <p><b>N+1 방지:</b>
 * AuctionResultRepository.findExpiredPendingPayments() 네이티브 쿼리에서
 * BID_HISTORY JOIN으로 배치 조회 → 상품별 개별 쿼리 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionPaymentScheduler {

    private static final int PAYMENT_HOURS = 12; // 차순위 결제 유예 시간

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final NotificationService notificationService;

    /**
     * 10분마다 실행: 결제 만료된 낙찰 건을 탐지하여 승계 또는 최종 유찰 처리.
     */
    @Scheduled(fixedDelay = 10 * 60 * 1_000L) // 10분 (ms)
    public void checkExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();

        // 만료된 결제 대기 AuctionResult 배치 조회 (BID_HISTORY JOIN → N+1 방지)
        List<AuctionResult> expiredResults = auctionResultRepository.findExpiredPendingPayments(now);
        if (expiredResults.isEmpty()) {
            return;
        }

        log.info("[PaymentScheduler] 결제 만료 {}건 탐지", expiredResults.size());

        for (AuctionResult expiredResult : expiredResults) {
            try {
                processPaymentExpiry(expiredResult.getResultNo());
            } catch (Exception e) {
                log.error("[PaymentScheduler] resultNo={} 처리 오류: {}",
                        expiredResult.getResultNo(), e.getMessage(), e);
            }
        }
    }

    /**
     * 단건 결제 만료 처리 (독립 트랜잭션).
     * 실패 시 이 단건만 롤백 — 나머지 건은 계속 처리.
     *
     * @param resultNo 처리할 AuctionResult 번호
     */
    @Transactional
    public void processPaymentExpiry(Long resultNo) {

        // ── 1. 만료된 AuctionResult 재조회 (멱등성: 이미 처리됐는지 확인) ────────
        AuctionResult expiredResult = auctionResultRepository.findById(resultNo)
                .orElseThrow(() -> new IllegalArgumentException("AuctionResult를 찾을 수 없습니다: " + resultNo));

        if (!"배송대기".equals(expiredResult.getStatus())) {
            log.info("[PaymentScheduler] resultNo={} 이미 처리됨 (status={}), 건너뜁니다.",
                    resultNo, expiredResult.getStatus());
            return;
        }

        // ── 2. 만료된 낙찰 BidHistory 조회 ──────────────────────────────────────
        BidHistory expiredBid = bidHistoryRepository.findById(expiredResult.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "입찰 기록을 찾을 수 없습니다. bidNo=" + expiredResult.getBidNo()));

        Long productNo = expiredBid.getProductNo();

        // ── 3. 상품 조회 ────────────────────────────────────────────────────────
        Product product = productRepository.findByIdWithLock(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productNo));

        if (product.getStatus() != 3) {
            log.info("[PaymentScheduler] 상품 {} status={} → PENDING_PAYMENT 아님, 건너뜁니다.",
                    productNo, product.getStatus());
            return;
        }

        // ── 4. 현재 낙찰자 bid 결제 불이행 처리 ─────────────────────────────────
        //   isWinner=0 으로 변경 + AuctionResult 만료 표시
        expiredBid.setIsWinner(0);
        expiredBid.setIsCancelled(1);        // 대기열에서 제거 (재선택 방지)
        expiredBid.setCancelReason("결제 기한 만료 (12시간)");
        expiredResult.setStatus("결제기한만료");

        // ── 5. 차순위 입찰자 조회 ───────────────────────────────────────────────
        //   위에서 expiredBid.isCancelled=1 로 설정했으므로
        //   findFirstByProductNoAndIsCancelled(0, desc) 호출 시 자동으로 제외됨.
        Optional<BidHistory> successorOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(productNo, 0);

        if (successorOpt.isPresent()) {
            // ── Phase 2: 차순위 승계 ────────────────────────────────────────────
            BidHistory successor = successorOpt.get();
            successor.setIsWinner(1);

            // 새 AuctionResult 생성 (차순위자에게 12시간 부여)
            AuctionResult newResult = AuctionResult.builder()
                    .bidNo(successor.getBidNo())
                    .status("배송대기")
                    .paymentDueDate(LocalDateTime.now().plusHours(PAYMENT_HOURS))
                    .build();
            auctionResultRepository.save(newResult);

            // 상품 현재가 갱신 (차순위 가격으로)
            product.setCurrentPrice(successor.getBidPrice());
            product.setWinnerNo(successor.getMemberNo());

            log.info("[PaymentScheduler] Phase2 승계: productNo={}, newWinner={}, price={}",
                    productNo, successor.getMemberNo(), successor.getBidPrice());

            // 차순위자 알림 (격리)
            notifySuccessor(successor.getMemberNo(), product.getTitle(), productNo);

            // 판매자 알림 (승계 알림)
            notifySeller(product.getSellerNo(), product.getTitle(), productNo,
                    "낙찰자가 결제 기한 내 결제를 완료하지 않아 차순위 입찰자에게 낙찰 권한이 이전되었습니다.");

        } else {
            // ── Phase 3: 최종 유찰 ────────────────────────────────────────────
            product.setStatus(4); // CLOSED_FAILED
            product.setWinnerNo(null);

            log.info("[PaymentScheduler] Phase3 최종 유찰: productNo={}", productNo);

            // 판매자 알림 (격리)
            notifySeller(product.getSellerNo(), product.getTitle(), productNo,
                    "낙찰자가 결제하지 않아 경매가 최종 유찰되었습니다. " +
                            "보상금은 지급되었으며 수수료 없이 재등록하실 수 있습니다.");
        }
    }

    // ── 알림 발송 헬퍼 (예외 격리) ──────────────────────────────────────────────

    private void notifySuccessor(Long memberNo, String productTitle, Long productNo) {
        try {
            notificationService.sendAndSaveNotification(
                    memberNo, "bid",
                    "상위 낙찰자의 결제 불이행으로 [" + productTitle + "] 낙찰 권한이 승계되었습니다. " +
                            "12시간 내 결제를 완료해 주세요.",
                    "/won/" + productNo);
        } catch (Exception e) {
            log.warn("[PaymentScheduler] 차순위자 알림 실패 (상품 {}): {}", productNo, e.getMessage());
        }
    }

    private void notifySeller(Long sellerNo, String productTitle, Long productNo, String message) {
        try {
            notificationService.sendAndSaveNotification(
                    sellerNo, "bid", message, "/products/" + productNo);
        } catch (Exception e) {
            log.warn("[PaymentScheduler] 판매자 알림 실패 (상품 {}): {}", productNo, e.getMessage());
        }
    }
}
