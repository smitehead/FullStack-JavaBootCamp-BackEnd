package com.javajava.project.scheduler;

import com.javajava.project.entity.Product;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 종료 폴링 스케줄러 (백업용).
 * 30초마다 실행하여 Watchdog이 놓친 상품을 보완 처리.
 *
 * 트랜잭션 전략:
 * - 목록 조회만 담당 (@Transactional 없음)
 * - 실제 처리는 AuctionClosingService.processOne()에서 상품별 개별 트랜잭션
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final ProductRepository productRepository;
    private final AuctionClosingService auctionClosingService;

    @Scheduled(cron = "*/30 * * * * *")
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        List<Product> expiredProducts = productRepository.findByEndTimeBeforeAndStatus(now, 0);
        if (expiredProducts.isEmpty()) {
            return;
        }

        log.info("[Scheduler] {}개의 종료 대상 경매를 발견했습니다.", expiredProducts.size());

        for (Product product : expiredProducts) {
            try {
                auctionClosingService.processOne(product.getProductNo());
            } catch (Exception e) {
                log.error("[Scheduler] 상품 번호 {} 처리 중 오류 발생: {}",
                        product.getProductNo(), e.getMessage(), e);
            }
        }
    }
}
