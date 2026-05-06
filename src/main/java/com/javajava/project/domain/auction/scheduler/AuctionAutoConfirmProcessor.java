package com.javajava.project.domain.auction.scheduler;

import com.javajava.project.domain.auction.service.AuctionResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 7일 자동 구매 확정 단건 처리 서비스.
 *
 * <p>
 * Self-Invocation 방지를 위해 {@link AuctionAutoConfirmScheduler}에서
 * 분리된 독립 빈. {@code REQUIRES_NEW}로 스케줄러 호출마다 새 트랜잭션을 보장하여
 * 단건 실패가 다른 건에 영향을 주지 않도록 격리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionAutoConfirmProcessor {

    private final AuctionResultService auctionResultService;

    /**
     * 낙찰 후 7일 경과 시 자동 구매 확정 처리 (단건, 독립 트랜잭션).
     *
     * @param resultNo 처리할 AuctionResult PK
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoConfirm(Long resultNo) {
        auctionResultService.autoConfirmPurchase(resultNo);
    }
}
