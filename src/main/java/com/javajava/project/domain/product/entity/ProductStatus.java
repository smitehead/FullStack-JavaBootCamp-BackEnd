package com.javajava.project.domain.product.entity;

import java.util.Arrays;

/**
 * 경매 상품 진행 상태.
 * DB에는 code(Integer)로 저장된다. (기존 스키마 호환)
 */
public enum ProductStatus {
    ACTIVE(0, "active"),
    COMPLETED(1, "completed"),
    CANCELED(2, "canceled"),
    PENDING(3, "pending"),
    FAILED(4, "failed");

    private final int code;
    private final String label;

    ProductStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() { return code; }
    public String label() { return label; }

    public static ProductStatus fromCode(Integer code) {
        if (code == null) return ACTIVE;
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElse(ACTIVE);
    }
}
