//카드 등록 요청
package com.javajava.project.dto;

import lombok.Getter;

@Getter
public class BillingKeyRegisterRequestDto {
    //portone이 발급한 빌링키
    private String customerUid;
    //카드사 명
    private String cardName;
    //마스킹된 카드번호
    private String cardNo;
}
