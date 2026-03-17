package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "POINT_CHARGE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charge_seq")
    @SequenceGenerator(name = "charge_seq", sequenceName = "CHARGE_SEQ", allocationSize = 1)
    @Column(name = "CHARGE_NO")
    private Long chargeNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo; // 충전한 회원번호 (FK)

    @Column(name = "CARD_COMPANY", length = 30)
    private String cardCompany; // 카드사명 (PG사 응답값)

    @Column(name = "CARD_NO", length = 20)
    private String cardNo; // 마스킹된 카드번호

    @Column(name = "CHARGE_AMOUNT", nullable = false)
    private Long chargeAmount; // 실제 결제 금액(원화)

    @Column(name = "POINT_AMOUNT", nullable = false)
    private Long pointAmount; // 전환된 포인트 금액

    @Column(name = "DISCOUNT", nullable = false)
    private Long discount = 0L; // 할인 금액

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "성공"; // 결제 상태 (성공/실패/취소)

    @Column(name = "PG_TID", length = 100)
    private String pgTid; // PG사 거래번호

    @Column(name = "CHARGED_AT", nullable = false, updatable = false)
    private LocalDateTime chargedAt = LocalDateTime.now();
}