package com.javajava.project.domain.point.dto;

import lombok.Getter;

@Getter
public class BillingKeyRegisterRequestDto {
    private String cardNumber;  // 카드번호 (16자리, 하이픈 없이)
    private String expiry;      // 유효기간 YYYY-MM 형식
    private String birth;       // 생년월일 6자리
    private String pwd2digit;   // 비밀번호 앞 2자리
}