package com.javajava.project.domain.point.dto;

import com.javajava.project.domain.point.entity.BankAccount;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class BankAccountDto {
    private Long accountNo;

    @NotBlank(message = "은행명은 필수입니다.")
    private String bankName;

    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNumber;

    @NotBlank(message = "예금주명은 필수입니다.")
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
