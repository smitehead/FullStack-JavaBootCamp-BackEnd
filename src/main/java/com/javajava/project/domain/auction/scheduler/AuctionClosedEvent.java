package com.javajava.project.domain.auction.scheduler;

/**
 * 경매 낙찰 완료 이벤트.
 * AuctionClosingService에서 트랜잭션 커밋 직전에 발행,
 * AuctionNotificationListener에서 커밋 완료 후 수신하여 알림 발송.
 *
 * 엔티티 대신 primitive 값만 담는 이유:
 * 트랜잭션 종료 후 엔티티는 detached 상태가 되어 lazy 필드 접근 시 예외 발생 가능.
 */
public record AuctionClosedEvent(
        Long productNo,
        String productTitle,
        Long sellerNo,
        Long winnerMemberNo,
        Long winningBidNo
) {}
