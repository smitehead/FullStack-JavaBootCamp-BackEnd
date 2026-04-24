package com.javajava.project.domain.bid.event;

import com.javajava.project.domain.bid.service.AutoBidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 수동 입찰 커밋 후 자동입찰을 독립 트랜잭션으로 처리하는 리스너.
 *
 * <p><b>왜 AFTER_COMMIT + REQUIRES_NEW 인가:</b>
 * <ul>
 *   <li>AFTER_COMMIT: 수동 입찰이 롤백된 경우 자동입찰은 실행되지 않는다.</li>
 *   <li>REQUIRES_NEW: 자동입찰 실패가 수동 입찰 트랜잭션에 영향을 주지 않는다.
 *       또한 수동 입찰 트랜잭션이 이미 커밋되었으므로 Product 락 충돌이 없다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoBidEventListener {

    private final AutoBidService autoBidService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBidCommitted(AutoBidTriggerEvent event) {
        try {
            boolean fired = autoBidService.triggerAutoBids(
                    event.productNo(), event.bidPrice(), event.triggerMemberNo());
            if (fired) {
                log.info("[AutoBidListener] 자동입찰 처리 완료: productNo={}", event.productNo());
            }
        } catch (Exception e) {
            log.warn("[AutoBidListener] 자동입찰 처리 실패 (수동 입찰은 이미 커밋됨): productNo={}, cause={}",
                    event.productNo(), e.getMessage());
        }
    }
}
