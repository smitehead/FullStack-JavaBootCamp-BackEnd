package com.javajava.project.domain.point.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BANK_ACCOUNT")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bank_account_seq")
    @SequenceGenerator(name = "bank_account_seq", sequenceName = "BANK_ACCOUNT_SEQ", allocationSize = 1)
    @Column(name = "ACCOUNT_NO")
    private Long accountNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;

    @Column(name = "BANK_NAME", nullable = false, length = 30)
    private String bankName;

    @Column(name = "ACCOUNT_NUMBER", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "ACCOUNT_HOLDER", nullable = false, length = 20)
    private String accountHolder;

    @Builder.Default
    @Column(name = "IS_DEFAULT", nullable = false)
    private Integer isDefault = 0;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}