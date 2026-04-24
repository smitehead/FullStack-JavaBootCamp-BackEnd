package com.javajava.project.domain.bid.event;

/**
 * 수동 입찰 트랜잭션 커밋 후 자동입찰 처리를 위한 이벤트.
 *
 * <p>{@code TransactionPhase.AFTER_COMMIT} 리스너가 수신하므로,
 * 수동 입찰이 성공적으로 커밋된 경우에만 자동입찰이 시도된다.
 */
public record AutoBidTriggerEvent(Long productNo, Long bidPrice, Long triggerMemberNo) {}
