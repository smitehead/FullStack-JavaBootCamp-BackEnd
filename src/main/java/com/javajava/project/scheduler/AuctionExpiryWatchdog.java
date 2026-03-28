package com.javajava.project.scheduler;

import com.javajava.project.entity.Product;
import com.javajava.project.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 경매 종료 정각 처리 Watchdog.
 *
 * 역할:
 * - 상품 등록 시 endTime에 딱 맞춰 낙찰 처리를 예약 (scheduleClose 호출)
 * - 서버 재시작 시 DB에서 미종료 상품을 읽어 자동 복구 (@PostConstruct)
 *
 * AuctionScheduler(30초 폴링)와 역할 분담:
 * - Watchdog → endTime 정각에 즉시 처리 (주)
 * - AuctionScheduler → Watchdog이 놓친 상품 보완 (백업)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionExpiryWatchdog {

    private final TaskScheduler taskScheduler;
    private final ProductRepository productRepository;
    private final AuctionClosingService auctionClosingService;

    // productNo → 예약된 ScheduledFuture (취소용)
    private final Map<Long, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();

    /**
     * 서버 재시작 시 아직 종료되지 않은 경매 상품을 자동 재예약.
     * endTime이 이미 지난 상품은 즉시(5초 후) 처리.
     */
    @PostConstruct
    public void recoverPendingAuctions() {
        List<Product> pending = productRepository.findByStatusAndEndTimeAfter(0, LocalDateTime.now());

        if (pending.isEmpty()) {
            log.info("[Watchdog] 재예약할 경매 없음.");
            return;
        }

        log.info("[Watchdog] 서버 재시작 복구 — {}개 경매 재예약", pending.size());
        for (Product product : pending) {
            scheduleClose(product.getProductNo(), product.getEndTime());
        }
    }

    /**
     * 상품 등록 시 호출. endTime에 낙찰 처리를 1회 예약.
     * endTime이 이미 지났으면 즉시(5초 후) 실행.
     *
     * @param productNo 상품 번호
     * @param endTime   경매 종료 시각
     */
    public void scheduleClose(Long productNo, LocalDateTime endTime) {
        // 기존 예약이 있으면 취소 (상품 수정 등 재등록 시 중복 방지)
        cancel(productNo);

        Instant fireAt = endTime.atZone(ZoneId.systemDefault()).toInstant();

        // endTime이 이미 지났으면 5초 후 즉시 처리
        if (fireAt.isBefore(Instant.now())) {
            fireAt = Instant.now().plusSeconds(5);
        }

        Instant finalFireAt = fireAt;
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    log.info("[Watchdog] 상품 {} 낙찰 처리 시작", productNo);
                    try {
                        auctionClosingService.processOne(productNo);
                    } catch (Exception e) {
                        log.error("[Watchdog] 상품 {} 처리 실패: {}", productNo, e.getMessage(), e);
                    } finally {
                        pendingTasks.remove(productNo);
                    }
                },
                finalFireAt
        );

        pendingTasks.put(productNo, future);
        log.info("[Watchdog] 상품 {} → {} 낙찰 처리 예약 완료", productNo, endTime);
    }

    /**
     * 경매 취소/삭제 시 예약 취소.
     */
    public void cancel(Long productNo) {
        ScheduledFuture<?> existing = pendingTasks.remove(productNo);
        if (existing != null) {
            existing.cancel(false);
            log.info("[Watchdog] 상품 {} 예약 취소", productNo);
        }
    }
}
