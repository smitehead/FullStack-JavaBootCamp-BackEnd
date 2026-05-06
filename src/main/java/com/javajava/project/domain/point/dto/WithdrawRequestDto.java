package com.javajava.project.domain.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class WithdrawRequestDto {
    @NotNull(message = "출금 금액은 필수입니다.")
    @Min(value = 1000, message = "최소 출금 금액은 1,000원입니다.")
    private Long amount;

    private Long accountNo;       // 등록된 계좌 선택 시
    private String bankName;      // 직접 입력 시
    private String accountNumber;
    private String accountHolder;
}
