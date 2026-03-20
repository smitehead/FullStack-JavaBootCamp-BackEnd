package com.javajava.project.scheduler;

import com.javajava.project.entity.AuctionResult;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.AuctionResultRepository;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AuctionResultRepository auctionResultRepository;

    /**
     * 매 1분마다 종료된 경매를 체크하여 낙찰 처리
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 종료 시간이 지났지만 아직 활성 상태(1)인 상품 조회
        List<Product> expiredProducts = productRepository.findByEndTimeBeforeAndIsActive(now, 1);

        if (expiredProducts.isEmpty()) {
            return;
        }

        log.info("[Scheduler] {}개의 종료 대상 경매를 발견했습니다.", expiredProducts.size());

        for (Product product : expiredProducts) {
            // 2. 상품 상태를 비활성(0)으로 변경
            product.setIsActive(0);

            // 3. 해당 상품의 유효한 최고가 입찰 기록 조회
            Optional<BidHistory> winningBidOpt = bidHistoryRepository
                    .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(product.getProductNo(), 0);

            if (winningBidOpt.isPresent()) {
                BidHistory winningBid = winningBidOpt.get();
                
                // 4. 입찰 기록에 낙찰 확정 표시
                winningBid.setIsWinner(1);

                // 5. 최종 낙찰 결과 테이블에 기록 저장
                // ERD 구조에 따라 productNo, buyerNo, finalPrice 대신 bidNo 하나만 참조합니다.
                auctionResultRepository.save(AuctionResult.builder()
                        .bidNo(winningBid.getBidNo()) 
                        .status("배송대기")           
                        .build());
                
                log.info("[Scheduler] 상품 번호 {} 낙찰 성공 (입찰번호: {})", 
                        product.getProductNo(), winningBid.getBidNo());
            } else {
                // 입찰자가 없는 경우 유찰 처리
                log.info("[Scheduler] 상품 번호 {} 입찰자 없음으로 유찰되었습니다.", product.getProductNo());
            }
        }
    }
}