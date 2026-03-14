package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BID_HISTORY")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bid_seq")
    @SequenceGenerator(name = "bid_seq", sequenceName = "BID_SEQ", allocationSize = 1)
    @Column(name = "BID_NO")
    private Long bidNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo; // 입찰자 회원번호 (FK)

    @Column(name = "PRODUCT_NO", nullable = false)
    private Long productNo; // 입찰 대상 상품번호 (FK)

    @Column(name = "BID_PRICE", nullable = false)
    private Long bidPrice; // 입찰 금액

    @Column(name = "BID_TIME", nullable = false, updatable = false)
    private LocalDateTime bidTime = LocalDateTime.now(); // 입찰 시각

    @Column(name = "IS_CANCELLED", nullable = false)
    private Integer isCancelled = 0; // 취소 여부 (1: 취소)

    @Column(name = "CANCEL_REASON", length = 200)
    private String cancelReason; // 취소 사유

    @Column(name = "IS_AUTO", nullable = false)
    private Integer isAuto = 0; // 자동입찰 여부 (1: 자동)

    @Column(name = "IS_WINNER", nullable = false)
    private Integer isWinner = 0; // 낙찰 여부 (1: 최종 낙찰자)
}