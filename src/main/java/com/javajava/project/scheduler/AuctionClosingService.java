package com.javajava.project.scheduler;

import com.javajava.project.entity.AuctionResult;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.AuctionResultRepository;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 상품 1개의 경매 종료 처리 (낙찰 또는 유찰).
     * @Transactional — 이 메서드 단위로 트랜잭션이 열리고 닫힘.
     * 실패 시 이 상품만 롤백되며 다른 상품에 영향 없음.
     */
    @Transactional
    public void processOne(Long productNo) {
        // 트랜잭션 내에서 상품 재조회 (스케줄러에서 받은 엔티티는 다른 컨텍스트)
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productNo));

        // 동시 실행 방어: 이미 처리된 상품이면 건너뜀
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
            product.setStatus(1);
            product.setWinnerNo(winningBid.getMemberNo());
            auctionResultRepository.save(AuctionResult.builder()
                    .bidNo(winningBid.getBidNo())
                    .status("배송대기")
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
                    winningBid.getBidNo()
            ));

        } else {
            product.setStatus(2);
            log.info("[Scheduler] 상품 번호 {} 유찰 처리 (입찰자 없음)", productNo);
        }
    }
}
