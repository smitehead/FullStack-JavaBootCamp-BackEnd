package com.javajava.project.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "PRODUCT_SEQ", allocationSize = 1)
    @Column(name = "PRODUCT_NO")
    private Long productNo;

    @Column(name = "SELLER_NO", nullable = false)
    private Long sellerNo;

    @Column(name = "CATEGORY_NO", nullable = false)
    private Long categoryNo;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(name = "TRADE_TYPE", nullable = false, length = 10)
    private String tradeType;

    @Column(name = "TRADE_ADDR_SHORT", length = 50)
    private String tradeAddrShort;

    @Column(name = "TRADE_ADDR_DETAIL", length = 255)
    private String tradeAddrDetail;

    @Builder.Default
    @Column(name = "VIEW_COUNT", nullable = false)
    private Long viewCount = 0L;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "IS_DELETED", nullable = false)
    private Integer isDeleted = 0;

    @Column(name = "START_PRICE", nullable = false)
    private Long startPrice;

    @Column(name = "CURRENT_PRICE", nullable = false)
    private Long currentPrice;

    @Column(name = "BUYOUT_PRICE")
    private Long buyoutPrice;

    @Column(name = "MIN_BID_UNIT", nullable = false)
    private Long minBidUnit;

    @Builder.Default
    @Column(name = "BID_COUNT", nullable = false)
    private Long bidCount = 0L;

    @Column(name = "END_TIME", nullable = false)
    private LocalDateTime endTime;

    @Builder.Default
    @Column(name = "SHIPPING_FEE", nullable = false)
    private Long shippingFee = 0L;

    /**
     * 경매 진행 상태
     * 0 : active (진행중)
     * 1 : completed (낙찰 완료)
     * 2 : canceled (취소)
     */
    @Builder.Default
    @Column(name = "STATUS", nullable = false)
    private Integer status = 0;

    // 최종 낙찰자 회원번호. 낙찰 전에는 NULL
    @Column(name = "WINNER_NO")
    private Long winnerNo;

    /**
     * 재등록 시 원본(유찰된) 상품번호. 유찰 파생 이력 추적용.
     * SECTION 1 DB 마이그레이션(PARENT_PRODUCT_NO) 대응.
     */
    @Column(name = "PARENT_PRODUCT_NO")
    private Long parentProductNo;

    /**
     * 입찰 취소 시 누적된 위약금 풀.
     * 결제 완료 시 2.5%는 판매자에게, 2.5%는 구매자 할인으로 분배.
     * 최종 유찰(CLOSED_FAILED) 시 전액 판매자에게 지급.
     */
    @Builder.Default
    @Column(name = "PENALTY_POOL", nullable = false)
    private Long penaltyPool = 0L;
}