package com.javajava.project.domain.point.dto;
import lombok.Getter;

@Getter
public class WithdrawRequestDto {
    private Long amount;
    private Long accountNo;      // 등록된 계좌 선택 시
    private String bankName;     // 직접 입력 시
    private String accountNumber;
    private String accountHolder;
}