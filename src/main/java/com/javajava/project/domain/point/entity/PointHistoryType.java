package com.javajava.project.domain.point.entity;

/**
 * PointHistory.type DB 저장 문자열 상수.
 * 기존 데이터 호환을 위해 실제 저장값은 변경하지 않는다.
 */
public final class PointHistoryType {
    public static final String BID_DEDUCT            = "입찰차감";
    public static final String BID_REFUND            = "입찰환불";
    public static final String BUYOUT_DEDUCT         = "즉시구매차감";
    public static final String BID_CANCEL_REFUND     = "입찰취소환불";
    public static final String PENALTY_DEDUCT        = "위약금차감";
    public static final String CANCEL_PENALTY        = "취소패널티";
    public static final String SETTLEMENT            = "낙찰대금수령";
    public static final String TRADE_CANCEL_RECOVERY = "거래취소회수";
    public static final String TRADE_CANCEL_REFUND   = "거래취소환불";
    public static final String CHARGE                = "충전";
    public static final String WITHDRAW              = "출금";

    private PointHistoryType() {}
}
