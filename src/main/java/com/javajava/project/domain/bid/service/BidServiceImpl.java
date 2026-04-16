package com.javajava.project.domain.bid.service;

import com.javajava.project.domain.bid.dto.BidRequestDto;
import com.javajava.project.domain.product.dto.ProductDetailResponseDto;
import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.auction.scheduler.AuctionClosingService;
import com.javajava.project.domain.auction.scheduler.AuctionClosedEvent;
import com.javajava.project.domain.auction.scheduler.AuctionExpiryWatchdog;
import com.javajava.project.domain.bid.entity.BidHistory;
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
@Transactional(readOnly = true)
public class BidServiceImpl implements BidService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final SseService sseService;
    private final NotificationService notificationService;
    private final AutoBidService autoBidService;
    private final AuctionClosingService auctionClosingService;
    private final AuctionExpiryWatchdog auctionExpiryWatchdog;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 입찰 프로세스 실행 (검증 + 포인트 환불/차감 + 상품 갱신 + 입찰 기록)
     */
    @Override
    @Transactional
    public String processBid(BidRequestDto bidDto) {
        // 1. 상품 정보 조회 (비관적 락)
        Product product = productRepository.findByIdWithLock(bidDto.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 2. 유효성 검증 (락 획득 전 빠른 실패 조건 먼저 체크)
        // [수정] isActive == 0 → status != 0 (0=active, 1=completed, 2=canceled)
        if (product.getStatus() != 0 || product.getEndTime().isBefore(LocalDateTime.now())) {
            return "이미 종료된 경매입니다.";
        }
        if (product.getSellerNo().equals(bidDto.getMemberNo())) {
            return "본인이 등록한 상품에는 입찰할 수 없습니다.";
        }
        // 재입찰 차단: 이 상품에 취소 이력이 있는 회원은 영구 차단
        if (bidHistoryRepository.existsByProductNoAndMemberNoAndIsCancelled(
                bidDto.getProductNo(), bidDto.getMemberNo(), 1)) {
            return "입찰을 취소한 상품에는 다시 입찰할 수 없습니다.";
        }
        long minRequiredBid = product.getCurrentPrice() + product.getMinBidUnit();
        if (bidDto.getBidPrice() < minRequiredBid) {
            return "최소 입찰 가능 금액은 " + minRequiredBid + "원입니다.";
        }

        // 3. 기존 최고 입찰 내역 확인
        Optional<BidHistory> lastBidOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(product.getProductNo(), 0);

        Long previousBidderNo = lastBidOpt.map(BidHistory::getMemberNo).orElse(null);

        if (previousBidderNo != null && previousBidderNo.equals(bidDto.getMemberNo())) {
            return "현재 최고 입찰자입니다. 추가 입찰이 불가합니다.";
        }

        // 4. 비관적 락 순서 고정 - memberNo 오름차순으로 항상 같은 순서 락 획득 (데드락 방지)
        Member currentBidder;
        Member previousBidder = null;

        if (previousBidderNo != null && previousBidderNo < bidDto.getMemberNo()) {
            // 이전 입찰자 번호가 더 작으면 → 이전 입찰자 먼저 락
            previousBidder = memberRepository.findByIdWithLock(previousBidderNo)
                    .orElseThrow(() -> new IllegalStateException("이전 입찰자 정보를 찾을 수 없습니다."));
            currentBidder = memberRepository.findByIdWithLock(bidDto.getMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        } else if (previousBidderNo != null) {
            // 현재 입찰자 번호가 더 작거나 같으면 → 현재 입찰자 먼저 락
            currentBidder = memberRepository.findByIdWithLock(bidDto.getMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
            previousBidder = memberRepository.findByIdWithLock(previousBidderNo)
                    .orElseThrow(() -> new IllegalStateException("이전 입찰자 정보를 찾을 수 없습니다."));
        } else {
            // 이전 입찰자 없으면 현재 입찰자만 락
            currentBidder = memberRepository.findByIdWithLock(bidDto.getMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        }

        // 5. 포인트 잔액 검증
        if (currentBidder.getPoints() < bidDto.getBidPrice()) {
            return "보유 포인트가 부족합니다.";
        }

        // 6. 이전 최고 입찰자 포인트 환불
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

            // SSE 발송 격리: SSE 오류가 트랜잭션 롤백을 유발하지 않음
            final long prevPoints = previousBidder.getPoints();
            final Long prevMemberNo = previousBidder.getMemberNo();
            try {
                sseService.sendPointUpdate(prevMemberNo, prevPoints);
            } catch (Exception e) {
                log.warn("[BidService] SSE 전송 실패: {}", e.getMessage());
            }

            try {
                String msg = String.format("상위 입찰 발생: %s에 더 높은 입찰가가 등록되었습니다.", product.getTitle());
                notificationService.sendAndSaveNotification(
                        prevMemberNo, "bid", msg, "/products/" + product.getProductNo());
            } catch (Exception e) {
                log.warn("[BidService] 알림 전송 실패: {}", e.getMessage());
            }
        }

        // 7. 현재 입찰자 포인트 차감
        currentBidder.setPoints(currentBidder.getPoints() - bidDto.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(currentBidder.getMemberNo())
                .type("입찰차감")
                .amount(-bidDto.getBidPrice())
                .balance(currentBidder.getPoints())
                .reason("[" + product.getTitle() + "] 경매 입찰 참여")
                .build());

        // SSE 포인트 업데이트 격리
        final long curPoints = currentBidder.getPoints();
        final Long curMemberNo = currentBidder.getMemberNo();
        try {
            sseService.sendPointUpdate(curMemberNo, curPoints);
        } catch (Exception ignored) {
            /* SSE 실패는 비즈니스 로직에 영향 없음 */ }

        // 8. 상품 및 입찰 기록 업데이트
        product.setCurrentPrice(bidDto.getBidPrice());
        product.setBidCount(product.getBidCount() + 1);

        BidHistory newBid = BidHistory.builder()
                .productNo(product.getProductNo())
                .memberNo(currentBidder.getMemberNo())
                .bidPrice(bidDto.getBidPrice())
                .bidTime(LocalDateTime.now())
                .isAuto(0)
                .isCancelled(0)
                .isWinner(0)
                .build();
        bidHistoryRepository.save(newBid);

        // 9. 판매자 알림 전송 (격리)
        try {
            String msgToSeller = String.format(
                    "새로운 입찰: 등록하신 %s에 새로운 입찰자가 등장했습니다.", product.getTitle());
            notificationService.sendAndSaveNotification(
                    product.getSellerNo(), "bid", msgToSeller, "/products/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[BidService] 알림 전송 실패: {}", e.getMessage());
        }

        // 10. 즉시구매가 도달 여부 확인 (입찰가 >= 즉시구매가 → 경매 즉시 종료)
        boolean buyoutTriggered = product.getBuyoutPrice() != null
                && bidDto.getBidPrice() >= product.getBuyoutPrice();

        if (buyoutTriggered) {
            // 경매 즉시 종료 (비관적 락 유지, 동일 트랜잭션)
            auctionClosingService.closeDueToBuyout(product, newBid);
            auctionExpiryWatchdog.cancel(product.getProductNo());
            try {
                sseService.broadcastBuyoutEnded(product.getProductNo(), bidDto.getBidPrice(), bidDto.getMemberNo());
            } catch (Exception e) {
                log.warn("[BidService] buyout SSE 브로드캐스트 실패: {}", e.getMessage());
            }
            log.info("[BidService] 입찰가 도달로 즉시구매 종료: productNo={}, price={}", product.getProductNo(),
                    bidDto.getBidPrice());
            return "SUCCESS";
        }

        // 11. 자동입찰 트리거 (방금 입찰한 사람 제외)
        //     자동입찰이 실행되면 내부에서 SSE를 직접 전송하므로, 수동 입찰의 중간 SSE는 생략한다.
        //     → 클라이언트가 "수동 입찰자가 1위" SSE를 받은 직후 "자동입찰자가 1위" SSE를 받아
        //       모달이 순간적으로 flicker 되는 문제를 근본적으로 차단.
        boolean autoBidFired = false;
        try {
            autoBidFired = autoBidService.triggerAutoBids(
                    product.getProductNo(), bidDto.getBidPrice(), bidDto.getMemberNo());
        } catch (Exception e) {
            log.warn("[BidService] 자동입찰 트리거 실패: {}", e.getMessage());
        }

        // 12. 자동입찰이 없었을 때만 수동 입찰 SSE 브로드캐스트 (격리)
        if (!autoBidFired) {
            try {
                sseService.broadcastPriceUpdate(product.getProductNo(), bidDto.getBidPrice(), bidDto.getMemberNo());
            } catch (Exception e) {
                log.warn("[BidService] 브로드캐스트 실패: {}", e.getMessage());
            }
        }

        return "SUCCESS";
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

        // 9. 상품 상태 업데이트
        product.setCurrentPrice(buyoutPrice);
        product.setStatus(1);
        product.setWinnerNo(memberNo);
        product.setBidCount(product.getBidCount() + 1);

        // 10. 낙찰 결과 저장
        auctionResultRepository.save(AuctionResult.builder()
                .bidNo(buyoutBid.getBidNo())
                .status("배송대기")
                .build());

        // 11. Watchdog 예약 취소 (즉시구매로 종료됐으므로 endTime 스케줄 불필요)
        auctionExpiryWatchdog.cancel(productNo);

        log.info("[Buyout] 즉시구매 완료: productNo={}, buyer={}, price={}", productNo, memberNo, buyoutPrice);

        // 12. 이벤트 발행 (커밋 후 알림 — AuctionNotificationListener 수신)
        eventPublisher.publishEvent(new AuctionClosedEvent(
                productNo, product.getTitle(), product.getSellerNo(), memberNo, buyoutBid.getBidNo()));

        // 13. SSE 경매 종료 브로드캐스트
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
    public List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo) {
        List<Object[]> results = bidHistoryRepository.findBidHistoryWithNickname(productNo);

        return results.stream().map(result -> {
            BidHistory bid = (BidHistory) result[0];
            String nickname = (String) result[1];

            return ProductDetailResponseDto.BidHistoryDto.builder()
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
    public List<BidHistory> getHistoryByProduct(Long productNo) {
        return bidHistoryRepository.findByProductNoOrderByBidTimeDesc(productNo);
    }
}