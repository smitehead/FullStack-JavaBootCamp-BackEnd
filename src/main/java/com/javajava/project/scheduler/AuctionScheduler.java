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
     * - status=0(진행중) 이면서 endTime이 지난 상품을 대상으로 함
     */
    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 종료 시간이 지났지만 아직 진행 중(status=0)인 상품 조회
        List<Product> expiredProducts = productRepository.findByEndTimeBeforeAndStatus(now, 0);

        if (expiredProducts.isEmpty()) {
            return;
        }

        log.info("[Scheduler] {}개의 종료 대상 경매를 발견했습니다.", expiredProducts.size());

        for (Product product : expiredProducts) {

            // 2. 해당 상품의 유효한 최고가 입찰 기록 조회
            Optional<BidHistory> winningBidOpt = bidHistoryRepository
                    .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(product.getProductNo(), 0);

            if (winningBidOpt.isPresent()) {
                BidHistory winningBid = winningBidOpt.get();

                // 3. 입찰 기록에 낙찰 확정 표시
                winningBid.setIsWinner(1);

                // 4. 상품 상태를 completed(1)로 변경 + 낙찰자 기록
                product.setStatus(1);
                product.setWinnerNo(winningBid.getMemberNo());

                // 5. 낙찰 결과 저장
                auctionResultRepository.save(AuctionResult.builder()
                        .bidNo(winningBid.getBidNo())
                        .status("배송대기")
                        .build());

                log.info("[Scheduler] 상품 번호 {} 낙찰 완료 (입찰번호: {}, 낙찰자: {})",
                        product.getProductNo(), winningBid.getBidNo(), winningBid.getMemberNo());

            } else {
                // 입찰자 없음 → 유찰(canceled=2) 처리
                product.setStatus(2);
                log.info("[Scheduler] 상품 번호 {} 유찰 처리 (입찰자 없음)", product.getProductNo());
            }
        }
    }
}