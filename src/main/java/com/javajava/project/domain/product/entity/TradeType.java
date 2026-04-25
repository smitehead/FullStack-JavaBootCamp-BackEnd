package com.javajava.project.domain.product.entity;

/**
 * Product.tradeType DB 저장 문자열 상수.
 * 기존 데이터 호환을 위해 실제 저장값은 변경하지 않는다.
 */
public final class TradeType {
    public static final String DIRECT   = "직거래";
    public static final String SHIPPING = "택배거래";
    public static final String BOTH     = "혼합";

    private TradeType() {}
}
