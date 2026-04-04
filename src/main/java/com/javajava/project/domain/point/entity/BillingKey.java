package com.javajava.project.domain.point.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_key")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_key_seq")
    @SequenceGenerator(name = "billing_key_seq", sequenceName = "BILLING_KEY_SEQ", allocationSize = 1)
    @Column(name = "BILLING_KEY_NO")
    private Long billingKeyNo;

    //회원당 1개만 허용 - DB UNIQUE 제약으로도 보장
    @Column(name = "MEMBER_NO", nullable = false, unique = true)
    private Long memberNo;

    //portone이 발급한 빌링키(자동결제)
    @Column(name = "CUSTOMER_UID", nullable = false, length=100)
    private String customerUid;

    //카드사 명(예: 신한, 국민카드 등)
    @Column(name = "CARD_NAME", length = 50)
    private String cardName;

    //카드번호 마스킹처리
    @Column(name = "CARD_NO", length=20)
    private String cardNo;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
