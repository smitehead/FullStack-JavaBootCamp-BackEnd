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

    // Builder 사용 시에도 현재 시각이 자동 할당되도록 설정
    @Builder.Default
    @Column(name = "BID_TIME", nullable = false, updatable = false)
    private LocalDateTime bidTime = LocalDateTime.now();

    // Builder 사용 시 기본값 0(취소 안됨)이 들어가도록 설정
    @Builder.Default
    @Column(name = "IS_CANCELLED", nullable = false)
    private Integer isCancelled = 0;

    @Column(name = "CANCEL_REASON", length = 200)
    private String cancelReason;

    //Builder 사용 시 기본값 0(수동 입찰)이 들어가도록 설정
    @Builder.Default
    @Column(name = "IS_AUTO", nullable = false)
    private Integer isAuto = 0;

    //Builder 사용 시 기본값 0(낙찰 아님)이 들어가도록 설정
    @Builder.Default
    @Column(name = "IS_WINNER", nullable = false)
    private Integer isWinner = 0;
}