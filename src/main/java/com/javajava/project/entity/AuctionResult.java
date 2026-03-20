package com.javajava.project.entity;

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

    @Column(name = "DELIVERY_EMD_NO")
    private Long deliveryEmdNo;

    @Column(name = "DELIVERY_ADDR_DETAIL", length = 255)
    private String deliveryAddrDetail;

    @Builder.Default
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "배송대기";

    @Column(name = "CONFIRMED_AT")
    private LocalDateTime confirmedAt;
}