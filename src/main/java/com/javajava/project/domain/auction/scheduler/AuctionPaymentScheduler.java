package com.javajava.project.domain.auction.scheduler;

import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
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
 *   <li>10분 주기로 status=3 (PENDING_PAYMENT) 상품 조회</li>
 *   <li>paymentDueDate 가 지난 AuctionResult 탐지</li>
 *   <li>현재 낙찰자를 결제기한만료 처리 후 포인트 충분한 다음 입찰자에게 12시간 부여</li>
 *   <li>승계된 낙찰자는 isForcePromoted=1 (이하 "탈출구 대상")</li>
 * </ul>
 *
 * <h3>Phase 3 — 최종 유찰 (탈출구)</h3>
 * <ul>
 *   <li>유효한 차순위 없으면 status=4 (CLOSED_FAILED)</li>
 *   <li>penaltyPool 전액 → 판매자 즉시 지급 (탈출구 보상)</li>
 *   <li>isForcePromoted=1 인 결제 불이행자에게는 매너온도 패널티 없음</li>
 * </ul>
 *
 * <h3>트랜잭션 전략</h3>
 * <p>목록 조회(checkExpiredPayments)와 단건 처리(processPaymentExpiry)를 분리.
 * 개별 처리 실패가 다른 건에 영향을 주지 않도록 독립 트랜잭션으로 실행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionPaymentScheduler {

    private static final int PAYMENT_HOURS = 12;

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final NotificationService notificationService;

    /**
     * 10분마다 실행: 결제 만료된 낙찰 건 탐지 → 승계 or 최종 유찰 처리.
     */
    @Scheduled(fixedDelay = 10 * 60 * 1_000L)
    public void checkExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionResult> expiredResults = auctionResultRepository.findExpiredPendingPayments(now);
        if (expiredResults.isEmpty()) return;

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
     * 실패 시 이 단건만 롤백, 나머지 건은 계속 처리됨.
     */
    @Transactional
    public void processPaymentExpiry(Long resultNo) {

        // ── 1. 만료된 AuctionResult 재조회 (멱등성 체크) ────────────────────────
        AuctionResult expiredResult = auctionResultRepository.findById(resultNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AuctionResult를 찾을 수 없습니다: " + resultNo));

        if (!"배송대기".equals(expiredResult.getStatus())) {
            log.info("[PaymentScheduler] resultNo={} 이미 처리됨 (status={}), 건너뜁니다.",
                    resultNo, expiredResult.getStatus());
            return;
        }

        // ── 2. 만료된 낙찰 BidHistory + 상품 조회 ───────────────────────────────
        BidHistory expiredBid = bidHistoryRepository.findById(expiredResult.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "입찰 기록을 찾을 수 없습니다. bidNo=" + expiredResult.getBidNo()));

        Long productNo = expiredBid.getProductNo();
        Product product = productRepository.findByIdWithLock(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productNo));

        if (product.getStatus() != 3) {
            log.info("[PaymentScheduler] 상품 {} status={} → PENDING_PAYMENT 아님, 건너뜁니다.",
                    productNo, product.getStatus());
            return;
        }

        // ── 3. 만료된 낙찰자 처리 ──────────────────────────────────────────────
        //   isForcePromoted=1 이면 강제 승계 대상 → 결제 불이행 시 매너 패널티 면제.
        boolean wasForcePromoted = Integer.valueOf(1).equals(expiredResult.getIsForcePromoted());
        expiredBid.setIsWinner(0);
        expiredBid.setIsCancelled(1);
        expiredBid.setCancelReason("결제 기한 만료 (12시간)" + (wasForcePromoted ? " [강제 승계 — 패널티 없음]" : ""));
        expiredResult.setStatus("결제기한만료");

        if (wasForcePromoted) {
            log.info("[PaymentScheduler] 강제 승계 대상 결제 불이행 → 매너 패널티 없음: memberNo={}",
                    expiredBid.getMemberNo());
        }

        // ── 4. 차순위 후보 스캔 (만료된 낙찰자 제외, 포인트 충분한 첫 번째 승계) ───
        List<BidHistory> candidates = bidHistoryRepository
                .findByProductNoAndIsCancelledAndMemberNoNotOrderByBidPriceDesc(
                        productNo, 0, expiredBid.getMemberNo());

        BidHistory successor = null;
        for (BidHistory candidate : candidates) {
            Member candidateMember = memberRepository.findByIdWithLock(candidate.getMemberNo())
                    .orElse(null);
            if (candidateMember == null) continue;

            if (candidateMember.getPoints() >= candidate.getBidPrice()) {
                // 포인트 충분 → 즉시 차감 후 강제 승계
                candidateMember.setPoints(candidateMember.getPoints() - candidate.getBidPrice());
                pointHistoryRepository.save(PointHistory.builder()
                        .memberNo(candidateMember.getMemberNo())
                        .type("입찰차감")
                        .amount(-candidate.getBidPrice())
                        .balance(candidateMember.getPoints())
                        .reason("[" + product.getTitle() + "] 스케줄러 강제 승계 입찰 차감")
                        .build());

                candidate.setIsWinner(1);

                AuctionResult newResult = AuctionResult.builder()
                        .bidNo(candidate.getBidNo())
                        .status("배송대기")
                        .paymentDueDate(LocalDateTime.now().plusHours(PAYMENT_HOURS))
                        .isForcePromoted(1) // 강제 승계 → 결제 불이행 시 매너 패널티 면제
                        .build();
                auctionResultRepository.save(newResult);

                product.setCurrentPrice(candidate.getBidPrice());
                product.setWinnerNo(candidate.getMemberNo());

                successor = candidate;
                log.info("[PaymentScheduler] Phase2 강제 승계: productNo={}, newWinner={}, price={}",
                        productNo, candidate.getMemberNo(), candidate.getBidPrice());

                notifySuccessor(candidate.getMemberNo(), product.getTitle(), productNo);
                notifySeller(product.getSellerNo(), product.getTitle(), productNo,
                        "낙찰자가 결제 기한 내 결제를 완료하지 않아 차순위 입찰자에게 낙찰 권한이 이전되었습니다.");
                break;

            } else {
                // 포인트 부족 → 건너뜀 (대기열에서 제거)
                candidate.setIsCancelled(1);
                candidate.setCancelReason("잔액 부족으로 스케줄러 승계 건너뜀");
                log.info("[PaymentScheduler] 포인트 부족 건너뜀: bidNo={}, memberNo={}, 필요={}, 보유={}",
                        candidate.getBidNo(), candidate.getMemberNo(),
                        candidate.getBidPrice(), candidateMember.getPoints());
            }
        }

        if (successor == null) {
            // ── Phase 3: 최종 유찰 (탈출구) ──────────────────────────────────────
            product.setStatus(4); // CLOSED_FAILED
            product.setWinnerNo(null);

            // penaltyPool 전액 → 판매자 지급 (탈출구 보상금)
            long pool = product.getPenaltyPool();
            if (pool > 0) {
                Optional<Member> sellerOpt = memberRepository.findByIdWithLock(product.getSellerNo());
                sellerOpt.ifPresent(seller -> {
                    seller.setPoints(seller.getPoints() + pool);
                    pointHistoryRepository.save(PointHistory.builder()
                            .memberNo(seller.getMemberNo())
                            .type("취소보상금")
                            .amount(pool)
                            .balance(seller.getPoints())
                            .reason("[" + product.getTitle() + "] 최종 유찰 위약금 풀 전액 지급")
                            .build());
                    log.info("[PaymentScheduler] Phase3 판매자 보상금 지급: sellerNo={}, amount={}",
                            seller.getMemberNo(), pool);
                });
                product.setPenaltyPool(0L);
            }

            log.info("[PaymentScheduler] Phase3 최종 유찰: productNo={}", productNo);
            notifySeller(product.getSellerNo(), product.getTitle(), productNo,
                    "낙찰자가 결제하지 않아 경매가 최종 유찰되었습니다. " +
                            "위약금 보상금이 지급되었으며 수수료 없이 재등록하실 수 있습니다.");
        }
    }

    // ── 알림 헬퍼 (예외 격리) ────────────────────────────────────────────────────

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
