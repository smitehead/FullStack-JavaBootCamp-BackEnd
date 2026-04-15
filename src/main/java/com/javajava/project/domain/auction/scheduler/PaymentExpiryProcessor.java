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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 만료 단건 처리 서비스 (Phase 2 & Phase 3).
 *
 * <h3>설계 이유 — 왜 별도 서비스로 분리했는가?</h3>
 * <p>Spring의 {@code @Transactional}은 <b>프록시(AOP) 기반</b>으로 동작한다.
 * 같은 빈(bean) 내에서 {@code this.메서드()}로 호출하면 프록시를 우회하여
 * 어노테이션이 완전히 무시된다 (Self-Invocation 문제).
 *
 * <p>이전 구현({@code AuctionPaymentScheduler.processPaymentExpiry})은
 * 스케줄러 메서드({@code @Scheduled})가 같은 클래스 내 {@code @Transactional} 메서드를
 * {@code this.processPaymentExpiry(...)}로 호출했기 때문에 트랜잭션이 실제로 시작되지 않았다.
 * 결과적으로 포인트 차감 후 예외 발생 시 롤백이 일어나지 않아 데이터 정합성이 파괴될 수 있었다.
 *
 * <h3>REQUIRES_NEW 선택 이유</h3>
 * <p>가이드라인은 단순 {@code @Transactional} 분리를 권고했지만,
 * {@code REQUIRES_NEW}를 명시적으로 사용한다.
 * 스케줄러는 트랜잭션 없이 호출하므로 결과는 동일하지만,
 * 향후 호출자 코드가 트랜잭션 내에서 이 메서드를 호출하더라도
 * <b>반드시 새로운 독립 트랜잭션</b>을 보장하여 다른 건의 롤백에 영향받지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExpiryProcessor {

    private static final int PAYMENT_HOURS = 12;

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final NotificationService notificationService;

    /**
     * 단건 결제 만료 처리.
     *
     * <p>{@code REQUIRES_NEW}: 호출자 트랜잭션과 완전히 분리된 독립 트랜잭션.
     * 이 단건 처리가 실패해도 다른 건에 영향을 주지 않는다.
     *
     * @param resultNo 처리할 AuctionResult PK
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long resultNo) {

        // ── 1. 멱등성 체크: 이미 처리된 건인지 확인 ─────────────────────────────
        AuctionResult expiredResult = auctionResultRepository.findById(resultNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AuctionResult를 찾을 수 없습니다: " + resultNo));

        if (!"배송대기".equals(expiredResult.getStatus())) {
            log.info("[PaymentExpiry] resultNo={} 이미 처리됨 (status={}), 건너뜁니다.",
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
            log.info("[PaymentExpiry] 상품 {} status={} → PENDING_PAYMENT 아님, 건너뜁니다.",
                    productNo, product.getStatus());
            return;
        }

        // ── 3. 만료된 낙찰자 처리 ──────────────────────────────────────────────
        //   isForcePromoted=1: 강제 승계 대상 → 결제 불이행 시 매너 패널티 면제.
        boolean wasForcePromoted = Integer.valueOf(1).equals(expiredResult.getIsForcePromoted());
        expiredBid.setIsWinner(0);
        expiredBid.setIsCancelled(1);
        expiredBid.setCancelReason("결제 기한 만료 (12시간)" +
                (wasForcePromoted ? " [강제 승계 — 매너 패널티 없음]" : ""));
        expiredResult.setStatus("결제기한만료");

        if (wasForcePromoted) {
            log.info("[PaymentExpiry] 강제 승계 대상 결제 불이행 → 매너 패널티 없음: memberNo={}",
                    expiredBid.getMemberNo());
        }

        // ── 4. 차순위 후보 스캔 (만료 낙찰자 제외, 포인트 충분한 첫 번째 승계) ────
        List<BidHistory> candidates = bidHistoryRepository
                .findByProductNoAndIsCancelledAndMemberNoNotOrderByBidPriceDesc(
                        productNo, 0, expiredBid.getMemberNo());

        BidHistory successor = findAndPromoteSuccessor(candidates, product);

        if (successor != null) {
            // ── Phase 2: 차순위 강제 승계 ────────────────────────────────────────
            AuctionResult newResult = AuctionResult.builder()
                    .bidNo(successor.getBidNo())
                    .status("배송대기")
                    .paymentDueDate(LocalDateTime.now().plusHours(PAYMENT_HOURS))
                    .isForcePromoted(1) // 강제 승계 → 결제 불이행 시 매너 패널티 면제
                    .build();
            auctionResultRepository.save(newResult);

            product.setCurrentPrice(successor.getBidPrice());
            product.setWinnerNo(successor.getMemberNo());

            log.info("[PaymentExpiry] Phase2 강제 승계: productNo={}, newWinner={}, price={}",
                    productNo, successor.getMemberNo(), successor.getBidPrice());

            notifySuccessor(successor.getMemberNo(), product.getTitle(), productNo);
            notifySeller(product.getSellerNo(), product.getTitle(), productNo,
                    "낙찰자가 결제 기한 내 결제를 완료하지 않아 차순위 입찰자에게 낙찰 권한이 이전되었습니다.");

        } else {
            // ── Phase 3: 최종 유찰 (탈출구) ──────────────────────────────────────
            product.setStatus(4); // CLOSED_FAILED
            product.setWinnerNo(null);

            distributePenaltyPool(product);

            log.info("[PaymentExpiry] Phase3 최종 유찰: productNo={}", productNo);
            notifySeller(product.getSellerNo(), product.getTitle(), productNo,
                    "낙찰자가 결제하지 않아 경매가 최종 유찰되었습니다. " +
                            "위약금 보상금이 지급되었으며 수수료 없이 재등록하실 수 있습니다.");
        }
    }

    // ── private 헬퍼 ─────────────────────────────────────────────────────────────

    /**
     * 후보 목록에서 포인트 충분한 첫 번째 후보를 찾아 즉시 승계 처리.
     * 포인트 부족한 후보는 Soft Delete 처리 후 다음 후보로 넘어간다.
     */
    private BidHistory findAndPromoteSuccessor(List<BidHistory> candidates, Product product) {
        for (BidHistory candidate : candidates) {
            Member candidateMember = memberRepository.findByIdWithLock(candidate.getMemberNo())
                    .orElse(null);
            if (candidateMember == null) continue;

            if (candidateMember.getPoints() >= candidate.getBidPrice()) {
                // 포인트 충분 → 즉시 차감 후 승계
                candidateMember.setPoints(candidateMember.getPoints() - candidate.getBidPrice());
                pointHistoryRepository.save(PointHistory.builder()
                        .memberNo(candidateMember.getMemberNo())
                        .type("입찰차감")
                        .amount(-candidate.getBidPrice())
                        .balance(candidateMember.getPoints())
                        .reason("[" + product.getTitle() + "] 스케줄러 강제 승계 입찰 차감")
                        .build());

                candidate.setIsWinner(1);
                log.info("[PaymentExpiry] 차순위 승계 확정: bidNo={}, memberNo={}, price={}",
                        candidate.getBidNo(), candidate.getMemberNo(), candidate.getBidPrice());
                return candidate;

            } else {
                // 포인트 부족 → 대기열 제거
                candidate.setIsCancelled(1);
                candidate.setCancelReason("잔액 부족으로 스케줄러 승계 건너뜀");
                log.info("[PaymentExpiry] 포인트 부족 건너뜀: bidNo={}, memberNo={}, 필요={}, 보유={}",
                        candidate.getBidNo(), candidate.getMemberNo(),
                        candidate.getBidPrice(), candidateMember.getPoints());
            }
        }
        return null;
    }

    /**
     * penaltyPool 전액을 판매자에게 지급.
     * 최종 유찰(CLOSED_FAILED) 또는 경매 종료 시 호출.
     */
    private void distributePenaltyPool(Product product) {
        long pool = product.getPenaltyPool();
        if (pool <= 0) return;

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
            log.info("[PaymentExpiry] penaltyPool 판매자 지급: sellerNo={}, amount={}",
                    seller.getMemberNo(), pool);
        });
        product.setPenaltyPool(0L);
    }

    private void notifySuccessor(Long memberNo, String productTitle, Long productNo) {
        try {
            notificationService.sendAndSaveNotification(
                    memberNo, "bid",
                    "상위 낙찰자의 결제 불이행으로 [" + productTitle + "] 낙찰 권한이 승계되었습니다. " +
                            "12시간 내 결제를 완료해 주세요.",
                    "/won/" + productNo);
        } catch (Exception e) {
            log.warn("[PaymentExpiry] 차순위자 알림 실패 (상품 {}): {}", productNo, e.getMessage());
        }
    }

    private void notifySeller(Long sellerNo, String productTitle, Long productNo, String message) {
        try {
            notificationService.sendAndSaveNotification(
                    sellerNo, "bid", message, "/products/" + productNo);
        } catch (Exception e) {
            log.warn("[PaymentExpiry] 판매자 알림 실패 (상품 {}): {}", productNo, e.getMessage());
        }
    }
}
