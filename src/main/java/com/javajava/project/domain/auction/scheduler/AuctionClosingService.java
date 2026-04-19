package com.javajava.project.domain.auction.scheduler;

import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 경매 종료 단건 처리 서비스.
 * AuctionScheduler에서 상품별로 호출되며, 각 호출이 독립된 트랜잭션으로 실행된다.
 * → 특정 상품 처리 실패 시 해당 상품만 롤백되고 나머지 상품은 정상 처리된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionClosingService {

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 상품 1개의 경매 종료 처리 (낙찰 또는 유찰).
     * 
     * @Transactional — 이 메서드 단위로 트랜잭션이 열리고 닫힘.
     *                실패 시 이 상품만 롤백되며 다른 상품에 영향 없음.
     */
    @Transactional
    public void processOne(Long productNo) {
        // 비관적 락으로 상품 조회 (Watchdog+Scheduler 동시 접근 방지)
        Product product = productRepository.findByIdWithLock(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productNo));

        if (product.getStatus() != 0) {
            log.info("[Scheduler] 상품 번호 {} 이미 처리됨, 건너뜁니다. (status={})", productNo, product.getStatus());
            return;
        }

        Optional<BidHistory> winningBidOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(productNo, 0);

        if (winningBidOpt.isPresent()) {
            BidHistory winningBid = winningBidOpt.get();

            // DB 처리 (트랜잭션 내)
            winningBid.setIsWinner(1);
            // status=3 (PENDING_PAYMENT): 낙찰 확정 후 12시간 결제 대기
            product.setStatus(3);
            product.setWinnerNo(winningBid.getMemberNo());

            // 멱등성 보장: AuctionResult 중복 생성 방지 (Watchdog+Scheduler 동시 실행 race condition 대비)
            boolean resultExists = auctionResultRepository.findFirstByBidNo(winningBid.getBidNo()).isPresent();
            if (resultExists) {
                // 이미 다른 실행자가 처리 완료 — 알림까지 모두 건너뜀
                log.warn("[Scheduler] 상품 번호 {} 낙찰 결과 이미 존재 — 중복 처리 건너뜀", productNo);
                return;
            }

            auctionResultRepository.save(AuctionResult.builder()
                    .bidNo(winningBid.getBidNo())
                    .status("배송대기")
                    .paymentDueDate(LocalDateTime.now().plusHours(12)) // 결제 마감 12시간
                    .build());

            log.info("[Scheduler] 상품 번호 {} 낙찰 완료 (입찰번호: {}, 낙찰자: {})",
                    productNo, winningBid.getBidNo(), winningBid.getMemberNo());

            // 트랜잭션 커밋 후 알림 발송을 위해 이벤트 발행
            // AuctionNotificationListener가 AFTER_COMMIT 시점에 수신하여 처리
            eventPublisher.publishEvent(new AuctionClosedEvent(
                    product.getProductNo(),
                    product.getTitle(),
                    product.getSellerNo(),
                    winningBid.getMemberNo(),
                    winningBid.getBidNo()));

        } else {
            // 입찰자 없는 유찰 → status=4 (CLOSED_FAILED)
            product.setStatus(4);
            log.info("[Scheduler] 상품 번호 {} 유찰 처리 (입찰자 없음)", productNo);

            // [Fix #5] penaltyPool > 0 이면 판매자에게 전액 지급
            distributePenaltyPool(product);
        }
    }

    /**
     * penaltyPool 전액을 판매자에게 지급하고 풀을 초기화.
     * 유찰(CLOSED_FAILED) 확정 시점에 호출된다.
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
                    .reason("[" + product.getTitle() + "] 경매 종료 시 위약금 풀 정산")
                    .build());
            log.info("[Scheduler] penaltyPool 판매자 지급: productNo={}, sellerNo={}, amount={}",
                    product.getProductNo(), seller.getMemberNo(), pool);
        });
        product.setPenaltyPool(0L);
    }

    /**
     * 즉시구매 또는 입찰가 도달로 인한 경매 즉시 종료.
     * 이미 비관적 락이 걸린 Product/BidHistory 엔티티를 받아 처리하며,
     * 호출자 트랜잭션에 합류(REQUIRED) → 한 트랜잭션으로 원자적 처리.
     *
     * <p>멱등성 이중 보호:
     * <ul>
     *   <li>product.status != 0 → 이미 종료됨</li>
     *   <li>AuctionResult 중복 체크 → Scheduler·직접입찰 동시 실행 방어</li>
     * </ul>
     */
    @Transactional
    public void closeDueToBuyout(Product product, BidHistory winningBid) {
        if (product.getStatus() != 0) {
            log.info("[Buyout] 상품 {} 이미 종료됨 — 건너뜀", product.getProductNo());
            return;
        }
        boolean resultExists = auctionResultRepository.findFirstByBidNo(winningBid.getBidNo()).isPresent();
        if (resultExists) {
            log.warn("[Buyout] 상품 {} 낙찰 결과 이미 존재 — 중복 건너뜀", product.getProductNo());
            return;
        }

        winningBid.setIsWinner(1);
        // status=3 (PENDING_PAYMENT): 즉시구매도 결제 확인 전까지 대기 상태
        product.setStatus(3);
        product.setWinnerNo(winningBid.getMemberNo());
        product.setEndTime(LocalDateTime.now()); // [추가] 즉시구매 시 종료 시간 업데이트

        auctionResultRepository.save(AuctionResult.builder()
                .bidNo(winningBid.getBidNo())
                .status("배송대기")
                .paymentDueDate(LocalDateTime.now().plusHours(12)) // 결제 마감 12시간
                .build());

        log.info("[Buyout] 즉시구매 경매 종료: productNo={}, winner={}, price={}",
                product.getProductNo(), winningBid.getMemberNo(), winningBid.getBidPrice());

        // 트랜잭션 커밋 후 알림 발송 (AFTER_COMMIT 리스너가 수신)
        eventPublisher.publishEvent(new AuctionClosedEvent(
                product.getProductNo(),
                product.getTitle(),
                product.getSellerNo(),
                winningBid.getMemberNo(),
                winningBid.getBidNo()));
    }
}
