package com.javajava.project.domain.point.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "POINT_CHARGE")
@Getter
@Setter
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

    // 멱등성 처리를 위한 결제 고유 식별자
    // 형식: charge_{memberNo}_{timestamp}
    @Column(name = "MERCHANT_UID", unique = true, length = 100)
    private String merchantUid;

    // portone이 반환하는 결제 고유번호(검증/취소에 사용)
    @Column(name = "PG_TID", length = 100)
    private String pgTid;

    @Column(name = "CARD_COMPANY", length = 30)
    private String cardCompany; // 카드사명 (PG사 응답값)

    @Column(name = "CARD_NO", length = 20)
    private String cardNo; // 마스킹된 카드번호

    @Column(name = "CHARGE_AMOUNT", nullable = false)
    private Long chargeAmount; // 실제 결제 금액(원화)

    @Column(name = "POINT_AMOUNT", nullable = false)
    private Long pointAmount; // 전환된 포인트 금액

    @Builder.Default
    @Column(name = "DISCOUNT", nullable = false)
    private Long discount = 0L; // 할인 금액(마지막에 보고 삭제)

    // PENDING → SUCCESS 또는 FAILED
    @Builder.Default
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "PENDING";

    @Builder.Default
    @Column(name = "CHARGED_AT", nullable = false, updatable = false)
    private LocalDateTime chargedAt = LocalDateTime.now();
}