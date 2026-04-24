package com.javajava.project.domain.bid.event;

/**
 * 수동 입찰 트랜잭션 커밋 후 자동입찰 처리 및 SSE 브로드캐스트를 위한 이벤트.
 *
 * <p>{@code TransactionPhase.AFTER_COMMIT} 리스너가 수신하므로,
 * 수동 입찰이 성공적으로 커밋된 경우에만 처리된다.
 *
 * @param participantCount 수동 입찰 직후 갱신된 bidCount — SSE payload에 포함해 프론트 뱃지 즉시 갱신
 */
public record AutoBidTriggerEvent(Long productNo, Long bidPrice, Long triggerMemberNo, long participantCount) {}
