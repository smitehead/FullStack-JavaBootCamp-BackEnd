package com.javajava.project.domain.auction.scheduler;

import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 만료 감시 스케줄러 (Phase 2 & Phase 3 진입점).
 *
 * <h3>역할 분리</h3>
 * <p>이 클래스는 오직 "만료된 결제 대기 목록 조회"와 "단건 처리 위임"만 담당한다.
 * 실제 처리 로직({@code processPaymentExpiry})은 {@link PaymentExpiryProcessor}에 있다.
 *
 * <h3>Self-Invocation 문제 해결</h3>
 * <p>이전 구현에서는 {@code @Scheduled} 메서드와 {@code @Transactional} 메서드가
 * 같은 클래스에 있었다. Spring의 {@code @Transactional}은 AOP 프록시 기반이므로
 * {@code this.processPaymentExpiry()} 형태의 자기 호출에서는 프록시가 우회되어
 * 트랜잭션이 아예 시작되지 않는다.
 *
 * <p>해결: 처리 로직을 {@link PaymentExpiryProcessor} 빈으로 완전히 분리하여
 * 스케줄러에서 외부 빈 메서드 호출 → 프록시 정상 경유 → 트랜잭션 보장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionPaymentScheduler {

    private final AuctionResultRepository auctionResultRepository;
    private final PaymentExpiryProcessor processor; // ← 외부 빈 주입 (self-invocation 방지)

    /**
     * 10분마다 실행: 결제 만료된 낙찰 건 탐지 → 단건 처리 위임.
     *
     * <p>이 메서드 자체는 {@code @Transactional}이 없다.
     * 목록 조회와 처리는 별도 트랜잭션으로 분리되어,
     * 개별 건 처리 실패가 전체 배치에 영향을 주지 않는다.
     */
    @Scheduled(fixedDelay = 10 * 60 * 1_000L)
    public void checkExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionResult> expiredResults = auctionResultRepository.findExpiredPendingPayments(now);

        if (expiredResults.isEmpty()) return;

        log.info("[PaymentScheduler] 결제 만료 {}건 탐지", expiredResults.size());

        for (AuctionResult expiredResult : expiredResults) {
            try {
                // 외부 빈 메서드 호출 → AOP 프록시 경유 → @Transactional(REQUIRES_NEW) 정상 적용
                processor.process(expiredResult.getResultNo());
            } catch (Exception e) {
                // 단건 실패는 로그만 남기고 다음 건 계속 처리
                log.error("[PaymentScheduler] resultNo={} 처리 오류: {}",
                        expiredResult.getResultNo(), e.getMessage(), e);
            }
        }
    }
}
