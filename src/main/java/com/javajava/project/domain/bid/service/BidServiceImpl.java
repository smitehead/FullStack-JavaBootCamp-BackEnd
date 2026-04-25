package com.javajava.project.domain.bid.service;

import com.javajava.project.domain.bid.dto.BidRequestDto;
import com.javajava.project.domain.bid.dto.BidResultDto;
import com.javajava.project.domain.product.dto.ProductDetailResponseDto;
import com.javajava.project.domain.auction.scheduler.AuctionClosingService;
import com.javajava.project.domain.auction.scheduler.AuctionExpiryWatchdog;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.event.AutoBidTriggerEvent;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.point.entity.PointHistoryType;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.product.repository.ProductRepository;
import com.javajava.project.global.sse.SseService;
import com.javajava.project.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidServiceImpl implements BidService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SseService sseService;
    private final NotificationService notificationService;
    private final AuctionClosingService auctionClosingService;
    private final AuctionExpiryWatchdog auctionExpiryWatchdog;
    private final ApplicationEventPublisher eventPublisher;

    // 입찰 컨텍스트: 현재·이전 입찰자 + 마지막 입찰 기록
    private record BidContext(Member currentBidder, Member previousBidder, Optional<BidHistory> lastBid) {}
    // 데드락 방지를 위한 memberNo 오름차순 락 결과
    private record MemberPair(Member current, Member previous) {}

    /**
     * 입찰 프로세스 실행 (검증 + 포인트 환불/차감 + 상품 갱신 + 입찰 기록)
     *
     * <p><b>동시성 보장 전략:</b>
     * <ol>
     *   <li>트랜잭션 진입 즉시 {@code Product}에 {@code SELECT FOR UPDATE} 발행 — 이후 모든 검증·변경이 직렬화됨</li>
     *   <li>Member 락은 항상 memberNo 오름차순 — 데드락 방지</li>
     *   <li>자동입찰은 {@link AutoBidTriggerEvent} 발행 → 커밋 후 별도 트랜잭션으로 처리 —
     *       REQUIRED 합류로 인한 rollback-only 오염 차단</li>
     * </ol>
     */
    @Override
    @Transactional
    public BidResultDto processBid(BidRequestDto bidDto) {
        Product product = lockAndValidateProduct(bidDto);
        BidContext ctx = loadBidContext(product, bidDto);
        validateBidPrice(product, bidDto);

        refundPreviousBidder(product, ctx);
        deductCurrentBidder(product, bidDto, ctx);
        BidHistory newBid = saveBidAndUpdatePrice(product, bidDto);

        notifySeller(product);

        if (isBuyoutReached(product, bidDto)) {
            return finalizeBuyout(product, newBid, bidDto);
        }

        publishPostCommitEvent(product, bidDto);
        log.info("[BidService] 입찰 완료 (자동입찰 이벤트 예약): productNo={}, memberNo={}, price={}",
                product.getProductNo(), bidDto.getMemberNo(), bidDto.getBidPrice());
        return BidResultDto.builder()
                .autoBidFired(false)
                .finalBidderNo(bidDto.getMemberNo())
                .finalPrice(bidDto.getBidPrice())
                .build();
    }

    // ── processBid 분해 private 메서드 ────────────────────────────────────────

    private Product lockAndValidateProduct(BidRequestDto dto) {
        Product product = productRepository.findByIdWithLock(dto.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        if (product.getStatus() != 0 || product.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("이미 종료된 경매입니다.");
        }
        if (product.getSellerNo().equals(dto.getMemberNo())) {
            throw new IllegalStateException("본인이 등록한 상품에는 입찰할 수 없습니다.");
        }
        if (bidHistoryRepository.existsByProductNoAndMemberNoAndIsCancelled(
                dto.getProductNo(), dto.getMemberNo(), 1)) {
            throw new IllegalStateException("입찰을 취소한 상품에는 다시 입찰할 수 없습니다.");
        }
        return product;
    }

    private BidContext loadBidContext(Product product, BidRequestDto dto) {
        Optional<BidHistory> lastBidOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(product.getProductNo(), 0);
        Long previousNo = lastBidOpt.map(BidHistory::getMemberNo).orElse(null);
        if (previousNo != null && previousNo.equals(dto.getMemberNo())) {
            throw new IllegalStateException("현재 최고 입찰자입니다. 추가 입찰이 불가합니다.");
        }
        MemberPair pair = lockMembersOrdered(dto.getMemberNo(), previousNo);
        return new BidContext(pair.current(), pair.previous(), lastBidOpt);
    }

    // 데드락 방지: 항상 memberNo 오름차순으로 비관적 락 획득
    private MemberPair lockMembersOrdered(Long currentNo, Long previousNo) {
        if (previousNo == null) {
            return new MemberPair(loadCurrentMember(currentNo), null);
        }
        if (previousNo < currentNo) {
            Member prev = loadPreviousMember(previousNo);
            Member curr = loadCurrentMember(currentNo);
            return new MemberPair(curr, prev);
        }
        Member curr = loadCurrentMember(currentNo);
        Member prev = loadPreviousMember(previousNo);
        return new MemberPair(curr, prev);
    }

    private Member loadCurrentMember(Long memberNo) {
        return memberRepository.findByIdWithLock(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    private Member loadPreviousMember(Long memberNo) {
        return memberRepository.findByIdWithLock(memberNo)
                .orElseThrow(() -> new IllegalStateException("이전 입찰자 정보를 찾을 수 없습니다."));
    }

    private void validateBidPrice(Product product, BidRequestDto dto) {
        long minRequiredBid = product.getCurrentPrice() + product.getMinBidUnit();
        if (dto.getBidPrice() < minRequiredBid) {
            throw new IllegalArgumentException("최소 입찰 가능 금액은 " + minRequiredBid + "원입니다.");
        }
    }

    private void refundPreviousBidder(Product product, BidContext ctx) {
        if (ctx.previousBidder() == null || ctx.lastBid().isEmpty()) return;
        BidHistory lastBid = ctx.lastBid().get();
        Member prev = ctx.previousBidder();
        prev.refundPoints(lastBid.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(prev.getMemberNo())
                .type(PointHistoryType.BID_REFUND)
                .amount(lastBid.getBidPrice())
                .balance(prev.getPoints())
                .reason("[" + product.getTitle() + "] 상위 입찰 발생으로 인한 자동 환불")
                .build());
        try { sseService.sendPointUpdate(prev.getMemberNo(), prev.getPoints()); }
        catch (Exception e) { log.warn("[BidService] SSE 전송 실패: {}", e.getMessage()); }
        notifyOutbid(prev.getMemberNo(), product);
    }

    private void notifyOutbid(Long memberNo, Product product) {
        try {
            notificationService.sendAndSaveNotification(memberNo, "bid",
                    String.format("상위 입찰 발생: %s에 더 높은 입찰가가 등록되었습니다.", product.getTitle()),
                    "/products/" + product.getProductNo(), "newBid");
        } catch (Exception e) {
            log.warn("[BidService] 알림 전송 실패: {}", e.getMessage());
        }
    }

    private void deductCurrentBidder(Product product, BidRequestDto dto, BidContext ctx) {
        ctx.currentBidder().usePoints(dto.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(ctx.currentBidder().getMemberNo())
                .type(PointHistoryType.BID_DEDUCT)
                .amount(-dto.getBidPrice())
                .balance(ctx.currentBidder().getPoints())
                .reason("[" + product.getTitle() + "] 경매 입찰 참여")
                .build());
        try { sseService.sendPointUpdate(ctx.currentBidder().getMemberNo(), ctx.currentBidder().getPoints()); }
        catch (Exception ignored) { /* SSE 실패는 비즈니스에 영향 없음 */ }
    }

    private BidHistory saveBidAndUpdatePrice(Product product, BidRequestDto dto) {
        product.setCurrentPrice(dto.getBidPrice());
        product.setBidCount(product.getBidCount() + 1);
        return bidHistoryRepository.save(BidHistory.builder()
                .productNo(product.getProductNo())
                .memberNo(dto.getMemberNo())
                .bidPrice(dto.getBidPrice())
                .bidTime(LocalDateTime.now())
                .isAuto(0)
                .isCancelled(0)
                .isWinner(0)
                .build());
    }

    private void notifySeller(Product product) {
        try {
            notificationService.sendAndSaveNotification(
                    product.getSellerNo(), "bid",
                    String.format("새로운 입찰: 등록하신 %s에 새로운 입찰자가 등장했습니다.", product.getTitle()),
                    "/products/" + product.getProductNo(), "newBid");
        } catch (Exception e) {
            log.warn("[BidService] 알림 전송 실패: {}", e.getMessage());
        }
    }

    private boolean isBuyoutReached(Product product, BidRequestDto dto) {
        return product.getBuyoutPrice() != null && dto.getBidPrice() >= product.getBuyoutPrice();
    }

    private BidResultDto finalizeBuyout(Product product, BidHistory newBid, BidRequestDto dto) {
        auctionClosingService.closeDueToBuyout(product, newBid);
        auctionExpiryWatchdog.cancel(product.getProductNo());
        try {
            sseService.broadcastBuyoutEnded(product.getProductNo(), dto.getBidPrice(), dto.getMemberNo());
        } catch (Exception e) {
            log.warn("[BidService] buyout SSE 브로드캐스트 실패: {}", e.getMessage());
        }
        log.info("[BidService] 즉시구매 종료: productNo={}, price={}", product.getProductNo(), dto.getBidPrice());
        return BidResultDto.builder()
                .autoBidFired(false)
                .finalBidderNo(dto.getMemberNo())
                .finalPrice(dto.getBidPrice())
                .build();
    }

    private void publishPostCommitEvent(Product product, BidRequestDto dto) {
        eventPublisher.publishEvent(
                new AutoBidTriggerEvent(product.getProductNo(), dto.getBidPrice(),
                        dto.getMemberNo(), product.getBidCount()));
    }

    // ── processBuyout 이하 메서드 — Phase 3 스코프 외 (변경 없음) ──────────────

    /**
     * 즉시구매 처리 (버튼 직접 클릭)
     * 비관적 락 + 경매 즉시 종료 + 포인트 차감/환불 + 알림/SSE
     */
    @Override
    @Transactional
    public String processBuyout(Long productNo, Long memberNo) {
        // 1. 상품 비관적 락
        Product product = productRepository.findByIdWithLock(productNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 2. 유효성 검증
        if (product.getStatus() != 0 || product.getEndTime().isBefore(LocalDateTime.now())) {
            return "이미 종료된 경매입니다.";
        }
        if (product.getBuyoutPrice() == null) {
            return "즉시 구매가가 설정되지 않은 상품입니다.";
        }
        if (product.getSellerNo().equals(memberNo)) {
            return "본인이 등록한 상품은 즉시 구매할 수 없습니다.";
        }
        // 재구매 차단: 이 상품에 취소 이력이 있는 회원은 즉시구매도 차단
        if (bidHistoryRepository.existsByProductNoAndMemberNoAndIsCancelled(productNo, memberNo, 1)) {
            return "입찰을 취소한 상품에는 즉시 구매할 수 없습니다.";
        }

        long buyoutPrice = product.getBuyoutPrice();

        // 3. 현재 최고 입찰자 확인
        Optional<BidHistory> lastBidOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(productNo, 0);
        Long previousBidderNo = lastBidOpt.map(BidHistory::getMemberNo).orElse(null);

        // 4. 비관적 락 순서 고정 (memberNo 오름차순 — 데드락 방지)
        Member buyer;
        Member previousBidder = null;

        if (previousBidderNo != null && !previousBidderNo.equals(memberNo)) {
            if (previousBidderNo < memberNo) {
                previousBidder = memberRepository.findByIdWithLock(previousBidderNo)
                        .orElseThrow(() -> new IllegalStateException("이전 입찰자 정보를 찾을 수 없습니다."));
                buyer = memberRepository.findByIdWithLock(memberNo)
                        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
            } else {
                buyer = memberRepository.findByIdWithLock(memberNo)
                        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
                previousBidder = memberRepository.findByIdWithLock(previousBidderNo)
                        .orElseThrow(() -> new IllegalStateException("이전 입찰자 정보를 찾을 수 없습니다."));
            }
        } else {
            buyer = memberRepository.findByIdWithLock(memberNo)
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        }

        // 5. 포인트 잔액 검증
        if (buyer.getPoints() < buyoutPrice) {
            return "보유 포인트가 부족합니다. 즉시 구매가: " + buyoutPrice + "원";
        }

        // 6. 이전 최고 입찰자 환불
        if (previousBidder != null && lastBidOpt.isPresent()) {
            BidHistory lastBid = lastBidOpt.get();
            previousBidder.refundPoints(lastBid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(previousBidder.getMemberNo())
                    .type(PointHistoryType.BID_REFUND)
                    .amount(lastBid.getBidPrice())
                    .balance(previousBidder.getPoints())
                    .reason("[" + product.getTitle() + "] 즉시구매 발생으로 인한 입찰 환불")
                    .build());
            final long prevPoints = previousBidder.getPoints();
            final Long prevNo = previousBidder.getMemberNo();
            try { sseService.sendPointUpdate(prevNo, prevPoints); }
            catch (Exception e) { log.warn("[Buyout] SSE 포인트 전송 실패: {}", e.getMessage()); }
            try {
                notificationService.sendAndSaveNotification(prevNo, "bid",
                        "[" + product.getTitle() + "] 즉시구매로 경매가 종료되어 입찰금이 환불되었습니다.",
                        "/products/" + productNo, "auctionEnd");
            } catch (Exception e) { log.warn("[Buyout] 환불 알림 실패: {}", e.getMessage()); }
        }

        // 7. 구매자 포인트 차감
        buyer.usePoints(buyoutPrice);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(memberNo)
                .type(PointHistoryType.BUYOUT_DEDUCT)
                .amount(-buyoutPrice)
                .balance(buyer.getPoints())
                .reason("[" + product.getTitle() + "] 즉시 구매")
                .build());
        final long buyerPoints = buyer.getPoints();
        try { sseService.sendPointUpdate(memberNo, buyerPoints); }
        catch (Exception e) { log.warn("[Buyout] SSE 포인트 전송 실패: {}", e.getMessage()); }

        // 8. 입찰 기록 생성 (IS_WINNER=1)
        BidHistory buyoutBid = bidHistoryRepository.save(BidHistory.builder()
                .productNo(productNo)
                .memberNo(memberNo)
                .bidPrice(buyoutPrice)
                .bidTime(LocalDateTime.now())
                .isAuto(0)
                .isCancelled(0)
                .isWinner(1)
                .build());

        // 9. 상품 및 입찰 상태 최종 업데이트 (AuctionClosingService 위임)
        // - status=3(PENDING_PAYMENT), endTime=now(), AuctionResult 저장, AuctionClosedEvent 발행 포함
        auctionClosingService.closeDueToBuyout(product, buyoutBid);

        // 10. Watchdog 예약 취소 (즉시구매로 종료됐으므로 endTime 스케줄 불필요)
        auctionExpiryWatchdog.cancel(productNo);

        log.info("[Buyout] 즉시구매 완료: productNo={}, buyer={}, price={}", productNo, memberNo, buyoutPrice);

        // 11. SSE 경매 종료 브로드캐스트
        try {
            sseService.broadcastBuyoutEnded(productNo, buyoutPrice, memberNo);
        } catch (Exception e) {
            log.warn("[Buyout] SSE 브로드캐스트 실패: {}", e.getMessage()); }

        return "SUCCESS";
    }

    /**
     * 입찰 기록 조회 (상세 페이지 탭 전용)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo) {
        List<Object[]> results = bidHistoryRepository.findBidHistoryWithNickname(productNo);

        return results.stream().map(result -> {
            BidHistory bid = (BidHistory) result[0];
            String nickname = (String) result[1];

            return ProductDetailResponseDto.BidHistoryDto.builder()
                    .bidNo(bid.getBidNo())
                    .bidderNickname(nickname)
                    .bidPrice(bid.getBidPrice())
                    .bidTime(bid.getBidTime())
                    .build();
        }).toList();
    }

    /**
     * 입찰 취소 로직
     * 취소 시 해당 입찰자에게 포인트 환불 및 PointHistory 기록 추가
     */
    @Override
    @Transactional
    public void cancelBid(Long bidNo, String reason) {
        BidHistory bid = bidHistoryRepository.findById(bidNo)
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));

        if (bid.getIsCancelled() == 1) {
            throw new IllegalStateException("이미 취소된 입찰입니다.");
        }

        bid.setIsCancelled(1);
        bid.setCancelReason(reason);

        // 포인트 환불 처리
        Member bidder = memberRepository.findByIdWithLock(bid.getMemberNo())
                .orElseThrow(() -> new IllegalStateException("입찰자 정보를 찾을 수 없습니다."));

        bidder.refundPoints(bid.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(bidder.getMemberNo())
                .type(PointHistoryType.BID_CANCEL_REFUND)
                .amount(bid.getBidPrice())
                .balance(bidder.getPoints())
                .reason("입찰 취소 환불: " + (reason != null ? reason : "사유 없음"))
                .build());

        // 환불 SSE 알림 (격리)
        try {
            sseService.sendPointUpdate(bidder.getMemberNo(), bidder.getPoints());
        } catch (Exception e) {
            log.warn("[BidService] SSE 전송 실패: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void placeBid(BidHistory bid) {
        bidHistoryRepository.save(bid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidHistory> getHistoryByProduct(Long productNo) {
        return bidHistoryRepository.findByProductNoOrderByBidTimeDesc(productNo);
    }
}
