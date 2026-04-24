package com.javajava.project.domain.bid.event;

import com.javajava.project.domain.bid.service.AutoBidService;
import com.javajava.project.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 수동 입찰 커밋 후 자동입찰 처리 및 SSE 브로드캐스트를 담당하는 리스너.
 *
 * <p><b>왜 AFTER_COMMIT + REQUIRES_NEW 인가:</b>
 * <ul>
 *   <li>AFTER_COMMIT: 수동 입찰이 롤백된 경우 실행되지 않는다.</li>
 *   <li>REQUIRES_NEW: 자동입찰 실패가 수동 입찰 트랜잭션에 영향을 주지 않는다.
 *       수동 입찰이 이미 커밋되었으므로 Product 락 충돌도 없다.</li>
 * </ul>
 *
 * <p><b>SSE 발송 타이밍:</b> 수동 입찰 트랜잭션이 커밋된 이후에만 SSE가 발송되므로
 * 프론트가 SSE를 받고 fetchProduct()를 호출해도 DB 상태와 항상 일치한다.
 * <ul>
 *   <li>자동입찰 없음: 이 리스너에서 수동 입찰 가격으로 broadcastPriceUpdate</li>
 *   <li>자동입찰 발생: triggerAutoBids() 내부에서 자동입찰 최종 가격으로 broadcastPriceUpdate</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoBidEventListener {

    private final AutoBidService autoBidService;
    private final SseService sseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBidCommitted(AutoBidTriggerEvent event) {
        try {
            boolean fired = autoBidService.triggerAutoBids(
                    event.productNo(), event.bidPrice(), event.triggerMemberNo());
            if (fired) {
                // 자동입찰이 발생했으면 triggerAutoBids() 내부에서 SSE 발송 완료
                log.info("[AutoBidListener] 자동입찰 처리 완료: productNo={}", event.productNo());
            } else {
                // 자동입찰 없음 → 수동 입찰 SSE를 여기서 발송 (커밋 보장 이후)
                try {
                    sseService.broadcastPriceUpdate(
                            event.productNo(), event.bidPrice(),
                            event.triggerMemberNo(), event.participantCount());
                } catch (Exception e) {
                    log.warn("[AutoBidListener] SSE 브로드캐스트 실패: productNo={}, cause={}",
                            event.productNo(), e.getMessage());
                }
            }
        } catch (Exception e) {
            // 자동입찰 실패해도 수동 입찰은 이미 커밋됨 — SSE는 베스트 에포트
            log.warn("[AutoBidListener] 자동입찰 처리 실패 (수동 입찰은 이미 커밋됨): productNo={}, cause={}",
                    event.productNo(), e.getMessage());
            try {
                sseService.broadcastPriceUpdate(
                        event.productNo(), event.bidPrice(),
                        event.triggerMemberNo(), event.participantCount());
            } catch (Exception sse) {
                log.warn("[AutoBidListener] SSE 폴백 실패: productNo={}", event.productNo());
            }
        }
    }
}
