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
import java.util.Optional;

/**
 * 최고 입찰자 입찰 취소 서비스 (Phase 1).
 *
 * <p>처리 순서:
 * <ol>
 *   <li>비관적 락으로 Product 조회 — 동시 입찰/상태변경 차단</li>
 *   <li>유효성 검증 (최고입찰자 여부, 경매 진행 중, 위약금 납부 가능 여부)</li>
 *   <li>입찰가 환불 + 위약금 차감 (입찰자 PointHistory)</li>
 *   <li>위약금을 판매자에게 즉시 지급 (판매자 PointHistory)</li>
 *   <li>취소된 입찰 Soft-Delete (isCancelled=1)</li>
 *   <li>차순위 입찰가로 Product.currentPrice 갱신 (endTime 절대 변경 안 함)</li>
 *   <li>SSE 브로드캐스트 — 모든 구독자에게 가격 하락 즉시 전파</li>
 *   <li>BidCancelledEvent 발행 — AFTER_COMMIT 후 알림 발송 (트랜잭션과 분리)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidCancelService {

    private static final double PENALTY_RATE = 0.10; // 위약금 10%

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SseService sseService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 현재 로그인한 사용자가 자신의 최고 입찰을 취소한다.
     *
     * @param productNo          취소할 상품 번호 (프론트에서 전달)
     * @param requestingMemberNo 요청자 회원번호 (SecurityContext에서 추출)
     * @throws IllegalArgumentException 상품·입찰 기록을 찾을 수 없는 경우
     * @throws IllegalStateException    유효성 검증 실패 (최고입찰자 아님, 포인트 부족, 경매 종료 등)
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

        // ── 5. 위약금 계산 ───────────────────────────────────────────────────────
        long bidPrice = topBid.getBidPrice();
        long penalty = (long) (bidPrice * PENALTY_RATE);

        // ── 6. 입찰자 비관적 락 + 포인트 검증 ───────────────────────────────────
        //   현재 잔액(입찰 시 차감된 후 남은 금액)이 위약금을 납부하기에 충분한지 확인.
        //   입찰가 환불 후 위약금을 차감하므로, 실질 검증 기준:
        //   (현재 잔액 + 환불액) >= 위약금  →  항상 만족
        //   대신 "환불 없이 위약금만 추가로 낼 여력" 검증:
        //   현재 잔액 >= penalty  (bidPrice는 별도 환불되므로 문제 없음)
        Member bidder = memberRepository.findByIdWithLock(requestingMemberNo)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));

        if (bidder.getPoints() < penalty) {
            throw new IllegalStateException(String.format(
                    "위약금 납부 포인트가 부족합니다. 필요 위약금: %,d P, 현재 잔액: %,d P",
                    penalty, bidder.getPoints()));
        }

        // ── 7. 판매자 비관적 락 ─────────────────────────────────────────────────
        //   데드락 방지: memberNo 오름차순으로 일관된 락 획득 순서 유지
        Member seller;
        Long sellerNo = product.getSellerNo();
        if (sellerNo < requestingMemberNo) {
            seller = memberRepository.findByIdWithLock(sellerNo)
                    .orElseThrow(() -> new IllegalStateException("판매자 정보를 찾을 수 없습니다."));
        } else {
            // 이미 bidder 락 보유 중. sellerNo > requestingMemberNo이면 다시 락 요청 가능
            seller = memberRepository.findByIdWithLock(sellerNo)
                    .orElseThrow(() -> new IllegalStateException("판매자 정보를 찾을 수 없습니다."));
        }

        // ── 8. Soft Delete: 취소 입찰 논리 삭제 ────────────────────────────────
        topBid.setIsCancelled(1);
        topBid.setCancelReason("입찰자 본인 취소 (위약금 " + penalty + "P 차감)");

        // ── 9. 포인트 처리: 입찰가 환불 → 위약금 차감 ──────────────────────────
        // 9-1. 입찰가 환불 (입찰 시 차감된 금액 복원)
        bidder.setPoints(bidder.getPoints() + bidPrice);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(bidder.getMemberNo())
                .type("입찰환불")
                .amount(bidPrice)
                .balance(bidder.getPoints())
                .reason("[" + product.getTitle() + "] 입찰 취소로 인한 환불")
                .build());

        // 9-2. 위약금 차감
        bidder.setPoints(bidder.getPoints() - penalty);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(bidder.getMemberNo())
                .type("위약금차감")
                .amount(-penalty)
                .balance(bidder.getPoints())
                .reason("[" + product.getTitle() + "] 입찰 취소 위약금 (입찰가의 10%)")
                .build());

        // 9-3. 판매자에게 위약금 즉시 지급
        seller.setPoints(seller.getPoints() + penalty);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(sellerNo)
                .type("취소보상금")
                .amount(penalty)
                .balance(seller.getPoints())
                .reason("[" + product.getTitle() + "] 1등 입찰자 취소 보상금")
                .build());

        // ── 10. 차순위 입찰자 조회 및 Product 갱신 ─────────────────────────────
        //   topBid가 이미 isCancelled=1 이므로 다음 최고가 = 2등 입찰 기록
        Optional<BidHistory> successorOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(productNo, 0);

        Long newPrice;
        Long successorBidderNo = null;

        if (successorOpt.isPresent()) {
            BidHistory successor = successorOpt.get();
            newPrice = successor.getBidPrice();
            successorBidderNo = successor.getMemberNo();
            log.info("[BidCancel] 차순위 승계: productNo={}, successor={}, price={}",
                    productNo, successorBidderNo, newPrice);
        } else {
            // 차순위 없음 → 시작가로 초기화
            newPrice = product.getStartPrice();
            log.info("[BidCancel] 차순위 없음. 시작가로 초기화: productNo={}, startPrice={}", productNo, newPrice);
        }

        // endTime은 절대 변경하지 않음
        product.setCurrentPrice(newPrice);

        // ── 11. SSE 브로드캐스트 (격리: SSE 오류가 트랜잭션 롤백을 유발하면 안 됨) ──
        final Long finalSuccessorNo = successorBidderNo;
        final Long finalNewPrice = newPrice;

        try {
            sseService.broadcastBidCancelled(productNo, finalNewPrice, finalSuccessorNo);
        } catch (Exception e) {
            log.warn("[BidCancel] SSE 브로드캐스트 실패: {}", e.getMessage());
        }

        // 포인트 변경 SSE (취소한 입찰자)
        try {
            sseService.sendPointUpdate(bidder.getMemberNo(), bidder.getPoints());
        } catch (Exception e) {
            log.warn("[BidCancel] 입찰자 포인트 SSE 실패: {}", e.getMessage());
        }

        // 포인트 변경 SSE (판매자)
        try {
            sseService.sendPointUpdate(sellerNo, seller.getPoints());
        } catch (Exception e) {
            log.warn("[BidCancel] 판매자 포인트 SSE 실패: {}", e.getMessage());
        }

        // ── 12. BidCancelledEvent 발행 (AFTER_COMMIT 시점에 알림 발송) ──────────
        //   메인 트랜잭션 커밋 실패 시 이 이벤트는 수신되지 않으므로 알림 오발송 없음.
        eventPublisher.publishEvent(new BidCancelledEvent(
                productNo,
                product.getTitle(),
                sellerNo,
                requestingMemberNo,
                finalSuccessorNo,
                penalty));

        log.info("[BidCancel] 입찰 취소 완료: productNo={}, cancelledBy={}, penalty={}, newPrice={}",
                productNo, requestingMemberNo, penalty, newPrice);
    }
}
