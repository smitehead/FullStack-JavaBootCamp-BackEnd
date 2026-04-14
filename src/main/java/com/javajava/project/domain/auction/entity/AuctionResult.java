package com.javajava.project.domain.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUCTION_RESULT")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "result_seq")
    @SequenceGenerator(name = "result_seq", sequenceName = "RESULT_SEQ", allocationSize = 1)
    @Column(name = "RESULT_NO")
    private Long resultNo;

    @Column(name = "BID_NO", nullable = false) //FK
    private Long bidNo;

    @Column(name = "DELIVERY_ADDR_ROAD", length = 200)
    private String deliveryAddrRoad;

    @Column(name = "DELIVERY_ADDR_DETAIL", length = 255)
    private String deliveryAddrDetail;

    @Builder.Default
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "배송대기";

    @Column(name = "CONFIRMED_AT")
    private LocalDateTime confirmedAt;

    /**
     * 결제 마감 일시. 낙찰 확정 시 +12시간, 차순위 승계 시 다시 +12시간 갱신.
     * SECTION 3 DB 마이그레이션(PAYMENT_DUE_DATE) 대응.
     */
    @Column(name = "PAYMENT_DUE_DATE")
    private LocalDateTime paymentDueDate;
}