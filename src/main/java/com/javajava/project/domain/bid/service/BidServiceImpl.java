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

        // ── Step 1. Product 비관적 락 획득 (트랜잭션의 첫 번째 쿼리) ──────────
        // findByIdWithLock 은 SELECT FOR UPDATE + 3초 타임아웃.
        // 이 시점에 MVCC 스냅샷이 확정되므로 이후 모든 조회가 일관된 현재 버전을 읽는다.
        Product product = productRepository.findByIdWithLock(bidDto.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // ── Step 2. 상태 검증 (락 획득 후 — 경쟁 트랜잭션 커밋 반영 보장) ────
        if (product.getStatus() != 0 || product.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("이미 종료된 경매입니다.");
        }
        if (product.getSellerNo().equals(bidDto.getMemberNo())) {
            throw new IllegalStateException("본인이 등록한 상품에는 입찰할 수 없습니다.");
        }
        if (bidHistoryRepository.existsByProductNoAndMemberNoAndIsCancelled(
                bidDto.getProductNo(), bidDto.getMemberNo(), 1)) {
            throw new IllegalStateException("입찰을 취소한 상품에는 다시 입찰할 수 없습니다.");
        }

        // ── Step 3. 최소 입찰가 검증 (락된 Product.currentPrice 기준) ────────
        long minRequiredBid = product.getCurrentPrice() + product.getMinBidUnit();
        if (bidDto.getBidPrice() < minRequiredBid) {
            throw new IllegalArgumentException("최소 입찰 가능 금액은 " + minRequiredBid + "원입니다.");
        }

        // ── Step 4. 현재 최고 입찰자 조회 ──────────────────────────────────────
        // Product 락이 이미 확보된 상태이므로 다른 입찰 트랜잭션이 BidHistory를 추가할 수 없다.
        // MySQL InnoDB REPEATABLE READ 에서 첫 번째 SELECT FOR UPDATE 가 스냅샷을 확정하므로
        // 이 일반 SELECT 도 Product 락 획득 이후의 커밋된 데이터를 읽는다.
        Optional<BidHistory> lastBidOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(product.getProductNo(), 0);
        Long previousBidderNo = lastBidOpt.map(BidHistory::getMemberNo).orElse(null);

        if (previousBidderNo != null && previousBidderNo.equals(bidDto.getMemberNo())) {
            throw new IllegalStateException("현재 최고 입찰자입니다. 추가 입찰이 불가합니다.");
        }

        // ── Step 5. Member 비관적 락 (memberNo 오름차순 — 데드락 방지) ─────────
        Member currentBidder;
        Member previousBidder = null;

        if (previousBidderNo != null && previousBidderNo < bidDto.getMemberNo()) {
            previousBidder = memberRepository.findByIdWithLock(previousBidderNo)
                    .orElseThrow(() -> new IllegalStateException("이전 입찰자 정보를 찾을 수 없습니다."));
            currentBidder = memberRepository.findByIdWithLock(bidDto.getMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        } else if (previousBidderNo != null) {
            currentBidder = memberRepository.findByIdWithLock(bidDto.getMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
            previousBidder = memberRepository.findByIdWithLock(previousBidderNo)
                    .orElseThrow(() -> new IllegalStateException("이전 입찰자 정보를 찾을 수 없습니다."));
        } else {
            currentBidder = memberRepository.findByIdWithLock(bidDto.getMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        }

        // ── Step 6. 포인트 잔액 검증 ─────────────────────────────────────────
        if (currentBidder.getPoints() < bidDto.getBidPrice()) {
            throw new IllegalStateException("보유 포인트가 부족합니다.");
        }

        // ── Step 7. 이전 최고 입찰자 포인트 환불 ────────────────────────────
        if (previousBidder != null && lastBidOpt.isPresent()) {
            BidHistory lastBid = lastBidOpt.get();
            previousBidder.setPoints(previousBidder.getPoints() + lastBid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(previousBidder.getMemberNo())
                    .type("입찰환불")
                    .amount(lastBid.getBidPrice())
                    .balance(previousBidder.getPoints())
                    .reason("[" + product.getTitle() + "] 상위 입찰 발생으로 인한 자동 환불")
                    .build());

            final long prevPoints = previousBidder.getPoints();
            final Long prevMemberNo = previousBidder.getMemberNo();
            try {
                sseService.sendPointUpdate(prevMemberNo, prevPoints);
            } catch (Exception e) {
                log.warn("[BidService] SSE 전송 실패: {}", e.getMessage());
            }
            try {
                notificationService.sendAndSaveNotification(
                        prevMemberNo, "bid",
                        String.format("상위 입찰 발생: %s에 더 높은 입찰가가 등록되었습니다.", product.getTitle()),
                        "/products/" + product.getProductNo());
            } catch (Exception e) {
                log.warn("[BidService] 알림 전송 실패: {}", e.getMessage());
            }
        }

        // ── Step 8. 현재 입찰자 포인트 차감 ─────────────────────────────────
        currentBidder.setPoints(currentBidder.getPoints() - bidDto.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(currentBidder.getMemberNo())
                .type("입찰차감")
                .amount(-bidDto.getBidPrice())
                .balance(currentBidder.getPoints())
                .reason("[" + product.getTitle() + "] 경매 입찰 참여")
                .build());
        try {
            sseService.sendPointUpdate(currentBidder.getMemberNo(), currentBidder.getPoints());
        } catch (Exception ignored) { /* SSE 실패는 비즈니스에 영향 없음 */ }

        // ── Step 9. 상품 가격 갱신 + 입찰 기록 저장 ────────────────────────
        product.setCurrentPrice(bidDto.getBidPrice());
        product.setBidCount(product.getBidCount() + 1);

        BidHistory newBid = bidHistoryRepository.save(BidHistory.builder()
                .productNo(product.getProductNo())
                .memberNo(currentBidder.getMemberNo())
                .bidPrice(bidDto.getBidPrice())
                .bidTime(LocalDateTime.now())
                .isAuto(0)
                .isCancelled(0)
                .isWinner(0)
                .build());

        // ── Step 10. 판매자 알림 ─────────────────────────────────────────────
        try {
            notificationService.sendAndSaveNotification(
                    product.getSellerNo(), "bid",
                    String.format("새로운 입찰: 등록하신 %s에 새로운 입찰자가 등장했습니다.", product.getTitle()),
                    "/products/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[BidService] 알림 전송 실패: {}", e.getMessage());
        }

        // ── Step 11. 즉시구매가 도달 → 경매 즉시 종료 ───────────────────────
        if (product.getBuyoutPrice() != null && bidDto.getBidPrice() >= product.getBuyoutPrice()) {
            auctionClosingService.closeDueToBuyout(product, newBid);
            auctionExpiryWatchdog.cancel(product.getProductNo());
            try {
                sseService.broadcastBuyoutEnded(product.getProductNo(), bidDto.getBidPrice(), bidDto.getMemberNo());
            } catch (Exception e) {
                log.warn("[BidService] buyout SSE 브로드캐스트 실패: {}", e.getMessage());
            }
            log.info("[BidService] 즉시구매 종료: productNo={}, price={}", product.getProductNo(), bidDto.getBidPrice());
            return BidResultDto.builder()
                    .autoBidFired(false)
                    .finalBidderNo(bidDto.getMemberNo())
                    .finalPrice(bidDto.getBidPrice())
                    .build();
        }

        // ── Step 12. 수동 입찰 SSE 브로드캐스트 (자동입찰은 커밋 후 별도 SSE 전송) ─
        try {
            sseService.broadcastPriceUpdate(product.getProductNo(), bidDto.getBidPrice(), bidDto.getMemberNo());
        } catch (Exception e) {
            log.warn("[BidService] 브로드캐스트 실패: {}", e.getMessage());
        }

        // ── Step 13. 자동입찰 트리거 이벤트 발행 ──────────────────────────────
        // [핵심 수정] triggerAutoBids 를 동일 트랜잭션 내에서 직접 호출하지 않는다.
        //
        // 기존 방식의 문제:
        //   autoBidService.triggerAutoBids() 는 @Transactional(REQUIRED) → 현재 TX에 JOIN.
        //   JOIN된 메서드 내부에서 RuntimeException 발생 시, Spring TransactionInterceptor 가
        //   공유 트랜잭션 전체를 rollback-only 로 마킹한다.
        //   processBid 의 try-catch 가 예외를 삼켜도 rollback-only 플래그는 남아,
        //   processBid 커밋 시 UnexpectedRollbackException → 수동 입찰 전체 롤백.
        //
        // 해결책:
        //   TransactionPhase.AFTER_COMMIT 리스너가 수신하는 이벤트를 발행한다.
        //   이 이벤트는 현재 트랜잭션이 성공적으로 커밋된 뒤에만 처리되므로:
        //   ① 자동입찰 실패가 수동 입찰에 영향을 주지 않는다.
        //   ② 자동입찰은 커밋 후 새 트랜잭션에서 Product 락을 재획득해 안전하게 처리된다.
        //   ③ 커밋 전에는 이벤트가 발행되지 않아 자동입찰이 구 가격을 볼 위험이 없다.
        eventPublisher.publishEvent(
                new AutoBidTriggerEvent(product.getProductNo(), bidDto.getBidPrice(), bidDto.getMemberNo()));

        log.info("[BidService] 입찰 완료 (자동입찰 이벤트 예약): productNo={}, memberNo={}, price={}",
                product.getProductNo(), bidDto.getMemberNo(), bidDto.getBidPrice());

        return BidResultDto.builder()
                .autoBidFired(false)
                .finalBidderNo(bidDto.getMemberNo())
                .finalPrice(bidDto.getBidPrice())
                .build();
    }

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
            previousBidder.setPoints(previousBidder.getPoints() + lastBid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(previousBidder.getMemberNo())
                    .type("입찰환불")
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
                        "/products/" + productNo);
            } catch (Exception e) { log.warn("[Buyout] 환불 알림 실패: {}", e.getMessage()); }
        }

        // 7. 구매자 포인트 차감
        buyer.setPoints(buyer.getPoints() - buyoutPrice);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(memberNo)
                .type("즉시구매차감")
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

        bidder.setPoints(bidder.getPoints() + bid.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(bidder.getMemberNo())
                .type("입찰취소환불")
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