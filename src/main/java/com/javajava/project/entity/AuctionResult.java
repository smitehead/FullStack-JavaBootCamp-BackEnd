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
    private Long resultNo; // 낙찰결과번호 (PK)

    @Column(name = "BID_NO", nullable = false)
    private Long bidNo; // 낙찰된 입찰번호 (FK - BID_HISTORY 참조)

    @Column(name = "DELIVERY_EMD_NO")
    private Long deliveryEmdNo; // 배송지 읍면동번호 (FK - EMD 참조)

    @Column(name = "DELIVERY_ADDR_DETAIL", length = 255)
    private String deliveryAddrDetail; // 배송지 상세주소

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "배송대기"; // 거래 상태 (배송대기 -> 배송중 -> 구매확정 등)

    @Column(name = "CONFIRMED_AT")
    private LocalDateTime confirmedAt; // 구매확정일시 (확정 후 판매자 정산)
}