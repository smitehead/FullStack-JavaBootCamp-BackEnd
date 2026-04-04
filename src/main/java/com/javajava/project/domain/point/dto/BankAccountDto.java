package com.javajava.project.domain.point.dto;

import com.javajava.project.domain.point.entity.BankAccount;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class BankAccountDto {
    private Long accountNo;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private Integer isDefault;

    public static BankAccountDto from(BankAccount a) {
        return BankAccountDto.builder()
                .accountNo(a.getAccountNo())
                .bankName(a.getBankName())
                .accountNumber(a.getAccountNumber())
                .accountHolder(a.getAccountHolder())
                .isDefault(a.getIsDefault())
                .build();
    }
}