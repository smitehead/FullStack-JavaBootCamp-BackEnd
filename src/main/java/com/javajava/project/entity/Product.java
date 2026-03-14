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
    private Long sellerNo; // 판매자 회원번호 (FK)

    @Column(name = "CATEGORY_NO", nullable = false)
    private Long categoryNo; // 카테고리 번호 (FK)

    @Column(nullable = false, length = 100)
    private String title;

    @Lob // CLOB 타입 대응
    @Column(nullable = false)
    private String description;

    @Column(name = "TRADE_TYPE", nullable = false, length = 10)
    private String tradeType; // 택배/직거래/혼합

    @Column(name = "TRADE_EMD_NO")
    private Long tradeEmdNo; // 직거래 위치 읍면동번호 (FK)

    @Column(name = "TRADE_ADDR_DETAIL", length = 255)
    private String tradeAddrDetail;

    @Column(name = "VIEW_COUNT", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "IS_DELETED", nullable = false)
    private Integer isDeleted = 0; // 1이면 삭제

    @Column(name = "START_PRICE", nullable = false)
    private Long startPrice;

    @Column(name = "CURRENT_PRICE", nullable = false)
    private Long currentPrice;

    @Column(name = "BUYOUT_PRICE")
    private Long buyoutPrice; // 즉시구매가 (Nullable)

    @Column(name = "MIN_BID_UNIT", nullable = false)
    private Long minBidUnit;

    @Column(name = "BID_COUNT", nullable = false)
    private Long bidCount = 0L;

    @Column(name = "END_TIME", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive = 1; // 0이면 경매 마감
}