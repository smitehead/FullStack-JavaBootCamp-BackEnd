package com.javajava.project.domain.bid.service;

import com.javajava.project.domain.bid.entity.AutoBid;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.event.BidCancelledEvent;
import com.javajava.project.domain.bid.repository.AutoBidRepository;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.platform.entity.PlatformRevenue;
import com.javajava.project.domain.platform.repository.PlatformRevenueRepository;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.point.entity.PointHistoryType;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 최고 입찰자 입찰 취소 서비스 (Phase 1).
 *
 * <h3>처리 순서</h3>
 * <ol>
 *   <li>판매자 본인 취소 시도 차단 [Fix #4 — 자전거래 어뷰징 방지]</li>
 *   <li>비관적 락으로 Product 조회</li>
 *   <li>경매 상태 검증 + 12시간 하드 리밋 [Fix #2 — 서버사이드 검증]</li>
 *   <li>최고 입찰자 여부 검증</li>
 *   <li>위약금 5% 계산</li>
 *   <li>비관적 락 — memberNo 오름차순 획득 [Fix #3 — 데드락 방지]</li>
 *   <li>입찰 Soft-Delete → 포인트 환불/차감 → penaltyPool 적립</li>
 *   <li>차순위 후보 스캔 (while 루프)</li>
 *   <li>Product.currentPrice 갱신 (endTime 절대 불변)</li>
 *   <li>SSE 브로드캐스트 + BidCancelledEvent 발행</li>
 * </ol>
 *
 * <h3>위약금 풀(penaltyPool)</h3>
 * <ul>
 *   <li>입찰 취소마다 5% 위약금이 {@code product.penaltyPool}에 누적</li>
 *   <li>정상 낙찰·결제 완료 시: 2.5% → 판매자 / 2.5% → 낙찰자 할인</li>
 *   <li>최종 유찰(CLOSED_FAILED) 시: 전액 → 판매자 (AuctionClosingService / PaymentExpiryProcessor)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidCancelService {

    /** 입찰 취소 가능 최소 잔여 시간 (시간 단위). 이 시간 이내이면 서버에서도 차단. */
    // TODO: QA 완료 후 12L 로 복구
    private static final long CANCEL_BLOCK_HOURS = 0L;

    private static final double PENALTY_RATE = 0.05; // 위약금 5%

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AutoBidRepository autoBidRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PlatformRevenueRepository platformRevenueRepository;
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

        // ══════════════════════════════════════════════════════════════════════
        // [Fix #4] 판매자 본인 취소 시도 원천 차단 — 자전거래 어뷰징 방지
        //
        // 판매자가 다계정으로 고액 입찰 후 취소를 반복하면 penaltyPool을 부풀려
        // CLOSED_FAILED 시 셀프 착복이 가능하다.
        // Product 락 획득 전에 조회하여 빠르게 차단한다.
        //
        // ※ 입찰 등록 로직(BidService.processBid)에도 동일한 판매자 본인 입찰 차단 검증이
        //   반드시 존재해야 한다. 이 검증은 마지막 방어선 역할이다.
        // ══════════════════════════════════════════════════════════════════════
        Product productForSellerCheck = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        if (productForSellerCheck.getSellerNo().equals(requestingMemberNo)) {
            throw new IllegalStateException("판매자 본인이 등록한 상품에는 입찰 취소 기능을 사용할 수 없습니다.");
        }

        // ── 1. 비관적 락으로 상품 재조회 (동시 입찰/상태변경 차단) ────────────────
        Product product = productRepository.findByIdWithLock(productNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // ── 2. 경매 상태 검증 ───────────────────────────────────────────────────
        if (product.getStatus() != 0) {
            throw new IllegalStateException("진행 중인 경매에서만 입찰 취소가 가능합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (product.getEndTime().isBefore(now)) {
            throw new IllegalStateException("이미 마감된 경매입니다.");
        }

        // ══════════════════════════════════════════════════════════════════════
        // [Fix #2] 서버사이드 12시간 하드 리밋
        //
        // 프론트엔드에서 hoursLeft >= 12 조건으로 취소 버튼을 숨기지만,
        // 직접 API 호출(curl 등)로 우회 가능하므로 서버에서도 반드시 검증한다.
        // 락 획득 후 검증하므로 TOCTOU(Time-of-Check-Time-of-Use) 레이스 없음.
        // ══════════════════════════════════════════════════════════════════════
        long minutesLeft = ChronoUnit.MINUTES.between(now, product.getEndTime());
        if (minutesLeft < CANCEL_BLOCK_HOURS * 60) {
            throw new IllegalStateException(String.format(
                    "경매 마감 %d시간 이내에는 입찰 취소가 불가합니다. (현재 남은 시간: %d분)",
                    CANCEL_BLOCK_HOURS, minutesLeft));
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

        // ══════════════════════════════════════════════════════════════════════
        // [Fix #3] 비관적 락 획득 순서 정렬 — 데드락 방지
        //
        // 문제: Thread1이 (bidder=300, seller=100) 순으로 락을 잡고,
        //       Thread2가 (bidder=100, seller=300) 순으로 잡으면 교착 상태 발생.
        //
        // 해결: 항상 memberNo PK 오름차순으로 락을 획득한다.
        //   → 모든 스레드가 동일한 순서로 락을 요청하므로 데드락 원천 차단.
        //
        // 주의: 아래 while 루프의 후보 멤버 락도 이 트랜잭션 내에서 유지된다.
        //   후보 수가 많을수록 락 보유 시간이 길어져 잠재적 경합이 발생할 수 있다.
        //   이를 완화하기 위해 후보 탐색을 MAX_CANDIDATE_SCAN 건으로 제한한다.
        // ══════════════════════════════════════════════════════════════════════
        Long sellerNo = product.getSellerNo();
        Long firstLockNo = Math.min(requestingMemberNo, sellerNo);
        Long secondLockNo = Math.max(requestingMemberNo, sellerNo);

        Member first = memberRepository.findByIdWithLock(firstLockNo)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다. memberNo=" + firstLockNo));
        Member second = memberRepository.findByIdWithLock(secondLockNo)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다. memberNo=" + secondLockNo));

        // 락 순서와 관계없이 의미 있는 변수명으로 재할당
        Member bidder = requestingMemberNo.equals(firstLockNo) ? first : second;
        Member seller  = sellerNo.equals(firstLockNo) ? first : second;

        // ── 6. 위약금 납부 가능 여부 검증 ────────────────────────────────────────
        //   입찰 시 bidPrice가 이미 차감된 상태. 취소 시: 환불 → 위약금 차감.
        //   검증 기준: (현재 잔액 + bidPrice) >= penalty
        if (bidder.getPoints() + bidPrice < penalty) {
            throw new IllegalStateException(String.format(
                    "위약금 납부 포인트가 부족합니다. 필요: %,dP, 환불 후 잔액: %,dP",
                    penalty, bidder.getPoints() + bidPrice));
        }

        // ── 7. 취소자의 해당 상품 모든 활성 입찰 일괄 무효화 ─────────────────────
        //   단건(topBid)만 무효화하면 낮은 가격의 과거 입찰이 차순위로 다시 부상하는 버그 발생.
        //   취소자의 모든 isCancelled=0 입찰을 한꺼번에 Soft-Delete한다.
        List<BidHistory> allActiveBids = bidHistoryRepository
                .findByProductNoAndMemberNoAndIsCancelled(productNo, requestingMemberNo, 0);
        String cancelReason = "입찰자 본인 취소 (위약금 " + penalty + "P 차감, 5%)";
        for (BidHistory bid : allActiveBids) {
            bid.setIsCancelled(1);
            bid.setCancelReason(cancelReason);
        }
        log.info("[BidCancel] 취소자 활성 입찰 {}건 일괄 무효화: memberNo={}, productNo={}",
                allActiveBids.size(), requestingMemberNo, productNo);

        // ── 8. 포인트 처리 ────────────────────────────────────────────────────────
        // 8-1. 입찰가 환불
        bidder.refundPoints(bidPrice);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(bidder.getMemberNo())
                .type(PointHistoryType.BID_REFUND)
                .amount(bidPrice)
                .balance(bidder.getPoints())
                .reason("[" + product.getTitle() + "] 입찰 취소 환불")
                .build());

        // 8-2. 위약금 차감 (5%)
        bidder.usePoints(penalty);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(bidder.getMemberNo())
                .type(PointHistoryType.PENALTY_DEDUCT)
                .amount(-penalty)
                .balance(bidder.getPoints())
                .reason("[" + product.getTitle() + "] 입찰 취소 위약금 (입찰가의 5%)")
                .build());

        // 8-3. 위약금 → 플랫폼 수익 테이블에 즉시 INSERT
        platformRevenueRepository.save(PlatformRevenue.builder()
                .amount(penalty)
                .reason("[" + product.getTitle() + "] 입찰 취소 위약금 (입찰가의 5%)")
                .sourceMemberNo(requestingMemberNo)
                .relatedProductNo(productNo)
                .build());
        log.info("[BidCancel] 플랫폼 수익 귀속: productNo={}, amount={}P (입찰가의 5%)", productNo, penalty);

        // seller 변수 미사용 경고 방지 (데드락 방지를 위해 락 획득은 필수, 실제 포인트 변경은 없음)
        log.debug("[BidCancel] 판매자 락 획득 확인: sellerNo={}", seller.getMemberNo());

        // ── 8-4. 취소자 자동입찰 강제 비활성화 ──────────────────────────────────
        //   수동 입찰 취소 후 자동입찰이 살아있으면 즉시 차순위 스캔에서 재발동,
        //   → 취소자가 다시 최고입찰자가 되어 SSE 상태 충돌 및 프론트 오류 발생.
        autoBidRepository
                .findByMemberNoAndProductNoAndIsActive(requestingMemberNo, productNo, 1)
                .ifPresent(autoBid -> {
                    autoBid.setIsActive(0);
                    autoBid.setUpdatedAt(java.time.LocalDateTime.now());
                    log.info("[BidCancel] 취소자 자동입찰 강제 비활성화: memberNo={}, productNo={}",
                            requestingMemberNo, productNo);
                });

        // ── 9. 차순위 후보 스캔 ────────────────────────────────────────────────
        //   취소자의 모든 입찰을 제외 (memberNo 전체 제외).
        //   포인트 충분한 첫 번째 후보를 찾아 즉시 차감 후 승계.
        //   포인트 부족한 후보는 isCancelled=1 처리 후 다음으로 넘어간다.
        List<BidHistory> candidates = bidHistoryRepository
                .findByProductNoAndIsCancelledAndMemberNoNotOrderByBidPriceDesc(
                        productNo, 0, requestingMemberNo);

        BidHistory successor = null;
        for (BidHistory candidate : candidates) {
            Member candidateMember = memberRepository.findByIdWithLock(candidate.getMemberNo())
                    .orElse(null);
            if (candidateMember == null) continue;

            if (candidateMember.getPoints() >= candidate.getBidPrice()) {
                // 포인트 충분 → 즉시 차감 후 승계
                // (이전에 상위 입찰자에게 밀렸을 때 환불된 금액이 현재 잔액에 포함되어 있음)
                candidateMember.usePoints(candidate.getBidPrice());
                pointHistoryRepository.save(PointHistory.builder()
                        .memberNo(candidateMember.getMemberNo())
                        .type(PointHistoryType.BID_DEDUCT)
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
                // 포인트 부족 → Soft Delete 후 다음 후보로
                candidate.setIsCancelled(1);
                candidate.setCancelReason("잔액 부족으로 차순위 승계 건너뜀");
                log.info("[BidCancel] 포인트 부족 건너뜀: bidNo={}, memberNo={}, 필요={}P, 보유={}P",
                        candidate.getBidNo(), candidate.getMemberNo(),
                        candidate.getBidPrice(), candidateMember.getPoints());
            }
        }

        // ── 10. Product 현재가 갱신 (endTime 절대 변경 안 함) ───────────────────
        long newPrice;
        Long successorBidderNo = null;

        if (successor != null) {
            newPrice = successor.getBidPrice();
            successorBidderNo = successor.getMemberNo();
        } else {
            // 유효한 차순위 없음 → 시작가로 초기화
            newPrice = product.getStartPrice();
            log.info("[BidCancel] 유효 차순위 없음. 시작가로 초기화: productNo={}, startPrice={}P", productNo, newPrice);
        }
        product.setCurrentPrice(newPrice);

        // ── 11. SSE 브로드캐스트 ─────────────────────────────────────────────────
        try {
            sseService.sendPointUpdate(bidder.getMemberNo(), bidder.getPoints());
        } catch (Exception e) {
            log.warn("[BidCancel] 입찰자 포인트 SSE 실패: {}", e.getMessage());
        }

        final Long finalSuccessorNo = successorBidderNo;
        final long finalNewPrice = newPrice;
        try {
            sseService.broadcastBidCancelled(productNo, finalNewPrice, finalSuccessorNo);
        } catch (Exception e) {
            log.warn("[BidCancel] 가격 하락 SSE 브로드캐스트 실패: {}", e.getMessage());
        }

        // ── 12. BidCancelledEvent 발행 (AFTER_COMMIT — 알림 발송) ────────────────
        eventPublisher.publishEvent(new BidCancelledEvent(
                productNo, product.getTitle(), sellerNo,
                requestingMemberNo, finalSuccessorNo, penalty));

        log.info("[BidCancel] 취소 완료: productNo={}, cancelledBy={}, penalty={}P(5%→플랫폼수익), newPrice={}P",
                productNo, requestingMemberNo, penalty, newPrice);
    }
}
