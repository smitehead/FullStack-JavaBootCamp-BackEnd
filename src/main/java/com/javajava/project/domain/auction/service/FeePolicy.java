package com.javajava.project.domain.auction.service;

import com.javajava.project.domain.product.entity.TradeType;

/**
 * 수수료 정책 상수 및 계산 유틸.
 * 직거래 1%, 그 외(택배거래·혼합) 2%.
 */
public final class FeePolicy {
    public static final double DIRECT_TRADE_RATE = 0.01;
    public static final double SHIPPING_RATE     = 0.02;

    public static double rateFor(String tradeType) {
        return TradeType.DIRECT.equals(tradeType) ? DIRECT_TRADE_RATE : SHIPPING_RATE;
    }

    public static String labelFor(String tradeType) {
        return TradeType.DIRECT.equals(tradeType) ? "1%" : "2%";
    }

    private FeePolicy() {}
}
