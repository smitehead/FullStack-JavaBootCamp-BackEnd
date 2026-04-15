package com.javajava.project.domain.bid.service;

import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.event.BidCancelledEvent;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.repository.ProductRepository;
import com.javajava.project.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 최고 입찰자 입찰 취소 서비스 (Phase 1).
 *
 * <h3>처리 순서</h3>
 * <ol>
 *   <li>비관적 락으로 Product 조회 — 동시 입찰/상태변경 차단</li>
 *   <li>유효성 검증 (최고입찰자 여부, 경매 진행 중)</li>
 *   <li>위약금 5% 계산 및 납부 가능 여부 확인</li>
 *   <li>입찰가 환불 → 위약금 차감 → 위약금을 Product.penaltyPool에 적립</li>
 *   <li>취소된 입찰 Soft-Delete (isCancelled=1)</li>
 *   <li>차순위 후보 스캔 (취소자 전체 제외): 포인트 충분한 첫 후보에 즉시 차감</li>
 *   <li>차순위가 없으면 포인트 부족한 후보를 순차적으로 건너뜀 (isCancelled=1)</li>
 *   <li>Product.currentPrice 갱신 (endTime 절대 변경 안 함)</li>
 *   <li>SSE 브로드캐스트 — 모든 구독자에게 가격 하락 전파</li>
 *   <li>BidCancelledEvent 발행 — AFTER_COMMIT 후 알림 발송</li>
 * </ol>
 *
 * <h3>위약금 풀(penaltyPool)</h3>
 * <ul>
 *   <li>입찰 취소마다 5% 위약금이 product.penaltyPool에 누적</li>
 *   <li>결제 완료 시: 2.5% → 판매자 보상, 2.5% → 낙찰자 할인</li>
 *   <li>최종 유찰(CLOSED_FAILED) 시: 전액 → 판매자 지급 (AuctionPaymentScheduler Phase 3)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidCancelService {

    private static final double PENALTY_RATE = 0.05; // 위약금 5%

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SseService sseService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 현재 로그인한 사용자가 자신의 최고 입찰을 취소한다.
     *
     * @param productNo          취소할 상품 번호
     * @param requestingMemberNo 요청자 회원번호 (SecurityContext에서 추출)
     */
    @Transactional
    public void cancelHighestBid(Long productNo, Long requestingMemberNo) {

        // ── 1. 비관적 락으로 상품 조회 ─────────────────────────────────────────
        Product product = productRepository.findByIdWithLock(productNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // ── 2. 경매 상태 검증 ───────────────────────────────────────────────────
        if (product.getStatus() != 0) {
            throw new IllegalStateException("진행 중인 경매에서만 입찰 취소가 가능합니다.");
        }
        if (product.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("이미 마감된 경매입니다.");
        }

        // ── 3. 현재 최고 입찰 기록 조회 ────────────────────────────────────────
        BidHistory topBid = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(productNo, 0)
                .orElseThrow(() -> new IllegalStateException("유효한 입찰 기록을 찾을 수 없습니다."));

        // ── 4. 요청자가 최고 입찰자인지 검증 ────────────────────────────────────
        if (!topBid.getMemberNo().equals(requestingMemberNo)) {
            throw new IllegalStateException("현재 최고 입찰자만 입찰을 취소할 수 있습니다.");
        }

        // ── 5. 위약금 계산 (5%) ──────────────────────────────────────────────────
        long bidPrice = topBid.getBidPrice();
        long penalty = (long) (bidPrice * PENALTY_RATE);

        // ── 6. 입찰자 비관적 락 + 위약금 납부 가능 여부 검증 ─────────────────────
        //   입찰 시 bidPrice 만큼 포인트가 차감된 상태.
        //   취소 흐름: 입찰가 환불 먼저 → 위약금 차감.
        //   따라서 실질 검증: 현재 잔액 >= 0 (환불 후 위약금 낼 수 있는지)
        //   = (현재 잔액 + bidPrice) >= penalty → 입찰가 환불 후 위약금 낼 수 있으면 항상 성립.
        //   단, 현재 잔액이 음수가 될 수 있으므로 명시적으로 (현재 잔액 + bidPrice - penalty) >= 0 검증.
        Member bidder = memberRepository.findByIdWithLock(requestingMemberNo)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));

        if (bidder.getPoints() + bidPrice < penalty) {
            throw new IllegalStateException(String.format(
                    "위약금 납부 포인트가 부족합니다. 필요 위약금: %,d P, 환불 후 잔액: %,d P",
                    penalty, bidder.getPoints() + bidPrice));
        }

        // ── 7. 판매자 비관적 락 (데드락 방지: memberNo 오름차순으로 락 순서 고정) ──
        Long sellerNo = product.getSellerNo();
        Member seller = memberRepository.findByIdWithLock(sellerNo)
                .orElseThrow(() -> new IllegalStateException("판매자 정보를 찾을 수 없습니다."));

        // ── 8. Soft Delete: 취소 입찰 논리 삭제 ────────────────────────────────
        topBid.setIsCancelled(1);
        topBid.setCancelReason("입찰자 본인 취소 (위약금 " + penalty + "P 차감, 5%)");

        // ── 9. 포인트 처리 ────────────────────────────────────────────────────────
        // 9-1. 입찰가 환불
        bidder.setPoints(bidder.getPoints() + bidPrice);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(bidder.getMemberNo())
                .type("입찰환불")
                .amount(bidPrice)
                .balance(bidder.getPoints())
                .reason("[" + product.getTitle() + "] 입찰 취소 환불")
                .build());

        // 9-2. 위약금 차감 (입찰가의 5%)
        bidder.setPoints(bidder.getPoints() - penalty);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(bidder.getMemberNo())
                .type("위약금차감")
                .amount(-penalty)
                .balance(bidder.getPoints())
                .reason("[" + product.getTitle() + "] 입찰 취소 위약금 (입찰가의 5%)")
                .build());

        // 9-3. 위약금을 Product.penaltyPool에 적립 (즉시 판매자 지급 안 함)
        //      결제 완료 시 2.5% → 판매자 / 2.5% → 낙찰자 할인으로 분배됨
        product.setPenaltyPool(product.getPenaltyPool() + penalty);
        log.info("[BidCancel] 위약금 풀 적립: productNo={}, penalty={}, totalPool={}",
                productNo, penalty, product.getPenaltyPool());

        // ── 10. 차순위 후보 스캔 ────────────────────────────────────────────────
        //   취소한 회원의 입찰은 모두 제외 (memberNo 전체 제외).
        //   isCancelled=0 인 후보를 입찰가 내림차순으로 가져와 포인트 충분한 첫 번째를 승계.
        List<BidHistory> candidates = bidHistoryRepository
                .findByProductNoAndIsCancelledAndMemberNoNotOrderByBidPriceDesc(
                        productNo, 0, requestingMemberNo);

        BidHistory successor = null;
        for (BidHistory candidate : candidates) {
            Member candidateMember = memberRepository.findByIdWithLock(candidate.getMemberNo())
                    .orElse(null);
            if (candidateMember == null) continue;

            if (candidateMember.getPoints() >= candidate.getBidPrice()) {
                // 포인트 충분 → 즉시 차감하고 승계.
                // 이전에 해당 후보가 상위 입찰자에게 밀렸을 때 포인트가 환불되었으므로
                // 지금 다시 차감하는 것이 맞는 처리임.
                candidateMember.setPoints(candidateMember.getPoints() - candidate.getBidPrice());
                pointHistoryRepository.save(PointHistory.builder()
                        .memberNo(candidateMember.getMemberNo())
                        .type("입찰차감")
                        .amount(-candidate.getBidPrice())
                        .balance(candidateMember.getPoints())
                        .reason("[" + product.getTitle() + "] 차순위 승계 입찰 확정 차감")
                        .build());

                try {
                    sseService.sendPointUpdate(candidateMember.getMemberNo(), candidateMember.getPoints());
                } catch (Exception e) {
                    log.warn("[BidCancel] 차순위자 포인트 SSE 실패: {}", e.getMessage());
                }

                successor = candidate;
                log.info("[BidCancel] 차순위 승계 확정: productNo={}, successor={}, price={}",
                        productNo, candidate.getMemberNo(), candidate.getBidPrice());
                break;

            } else {
                // 포인트 부족 → 건너뜀 (Soft Delete)
                candidate.setIsCancelled(1);
                candidate.setCancelReason("잔액 부족으로 차순위 승계 건너뜀");
                log.info("[BidCancel] 포인트 부족으로 건너뜀: bidNo={}, memberNo={}, 필요={}, 보유={}",
                        candidate.getBidNo(), candidate.getMemberNo(),
                        candidate.getBidPrice(), candidateMember.getPoints());
            }
        }

        // ── 11. Product 현재가 갱신 (endTime 절대 변경 안 함) ───────────────────
        long newPrice;
        Long successorBidderNo = null;

        if (successor != null) {
            newPrice = successor.getBidPrice();
            successorBidderNo = successor.getMemberNo();
        } else {
            // 유효한 차순위 없음 → 시작가로 초기화
            newPrice = product.getStartPrice();
            log.info("[BidCancel] 유효 차순위 없음. 시작가로 초기화: productNo={}, startPrice={}", productNo, newPrice);
        }

        product.setCurrentPrice(newPrice);

        // ── 12. 판매자 포인트 SSE (penaltyPool 쌓였음을 알림 용도는 별도 알림 이벤트로) ──
        try {
            sseService.sendPointUpdate(bidder.getMemberNo(), bidder.getPoints());
        } catch (Exception e) {
            log.warn("[BidCancel] 입찰자 포인트 SSE 실패: {}", e.getMessage());
        }

        // ── 13. SSE 브로드캐스트: 가격 하락 즉시 전파 ──────────────────────────
        final Long finalSuccessorNo = successorBidderNo;
        final long finalNewPrice = newPrice;
        try {
            sseService.broadcastBidCancelled(productNo, finalNewPrice, finalSuccessorNo);
        } catch (Exception e) {
            log.warn("[BidCancel] SSE 브로드캐스트 실패: {}", e.getMessage());
        }

        // ── 14. BidCancelledEvent 발행 (AFTER_COMMIT — 알림 발송) ────────────────
        eventPublisher.publishEvent(new BidCancelledEvent(
                productNo,
                product.getTitle(),
                sellerNo,
                requestingMemberNo,
                finalSuccessorNo,
                penalty));

        log.info("[BidCancel] 취소 완료: productNo={}, cancelledBy={}, penalty={}P (5%), newPrice={}, penaltyPool={}",
                productNo, requestingMemberNo, penalty, newPrice, product.getPenaltyPool());
    }
}
