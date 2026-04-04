package com.javajava.project.domain.point.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "POINT_WITHDRAW")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointWithdraw {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "withdraw_seq")
    @SequenceGenerator(name = "withdraw_seq", sequenceName = "POINT_WITHDRAW_SEQ", allocationSize = 1)
    @Column(name = "WITHDRAW_NO")
    private Long withdrawNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Column(name = "BANK_NAME", nullable = false, length = 30)
    private String bankName;

    @Column(name = "ACCOUNT_NUMBER", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "ACCOUNT_HOLDER", nullable = false, length = 20)
    private String accountHolder;

    @Builder.Default
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "신청";

    @Column(name = "ADMIN_NO")
    private Long adminNo;

    @Column(name = "ADMIN_NICKNAME", length = 15)
    private String adminNickname;

    @Column(name = "REJECT_REASON", length = 200)
    private String rejectReason;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "PROCESSED_AT")
    private LocalDateTime processedAt;
}