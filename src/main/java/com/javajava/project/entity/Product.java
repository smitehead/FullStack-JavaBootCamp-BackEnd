package com.javajava.project.entity;

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

    @Column(name = "TRADE_EMD_NO")
    private Long tradeEmdNo;

    @Column(name = "TRADE_ADDR_DETAIL", length = 255)
    private String tradeAddrDetail;

    // ★ Builder 사용 시에도 기본값 0L이 들어가도록 설정
    @Builder.Default
    @Column(name = "VIEW_COUNT", nullable = false)
    private Long viewCount = 0L;

    // ★ 중요: Builder 사용 시 현재 시간이 자동으로 들어가도록 설정
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
    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive = 1;
}