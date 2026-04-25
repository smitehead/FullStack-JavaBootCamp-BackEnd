package com.javajava.project.domain.auction.entity;

/**
 * AuctionResult.status DB 저장 문자열 상수.
 * 기존 데이터 호환을 위해 실제 저장값은 변경하지 않는다.
 */
public final class AuctionResultStatus {
    public static final String AWAITING_SHIPMENT  = "배송대기";
    public static final String PURCHASE_CONFIRMED = "구매확정";
    public static final String TRADE_CANCELED     = "거래취소";
    public static final String PAYMENT_COMPLETED  = "결제완료";
    public static final String PAYMENT_EXPIRED    = "결제기한만료";

    private AuctionResultStatus() {}
}
