package com.javajava.project.domain.bid.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTO_BID")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AutoBid {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auto_bid_seq")
    @SequenceGenerator(name = "auto_bid_seq", sequenceName = "AUTO_BID_SEQ", allocationSize = 1)
    @Column(name = "AUTO_BID_NO")
    private Long autoBidNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;

    @Column(name = "PRODUCT_NO", nullable = false)
    private Long productNo;

    @Column(name = "MAX_PRICE", nullable = false)
    private Long maxPrice;

    @Builder.Default
    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive = 1;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
