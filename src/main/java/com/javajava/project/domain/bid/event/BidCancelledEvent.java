package com.javajava.project.domain.bid.event;

/**
 * 최고 입찰자 입찰 취소 완료 이벤트.
 *
 * BidCancelService에서 트랜잭션 커밋 직전 발행,
 * BidCancelNotificationListener에서 AFTER_COMMIT 시점에 수신하여 알림 발송.
 *
 * 엔티티 대신 primitive 값만 담는 이유:
 * 트랜잭션 종료 후 엔티티는 detached 상태가 되어 lazy 필드 접근 시 예외 발생 가능.
 *
 * @param productNo           상품 번호
 * @param productTitle        상품명
 * @param sellerNo            판매자 회원번호
 * @param cancelledBidderNo   취소한 입찰자(1등) 회원번호
 * @param successorBidderNo   차순위(2등) 입찰자 회원번호. 2등이 없으면 null.
 * @param penalty             위약금 금액 (현재 입찰가의 10%)
 */
public record BidCancelledEvent(
        Long productNo,
        String productTitle,
        Long sellerNo,
        Long cancelledBidderNo,
        Long successorBidderNo,
        long penalty
) {}
