//충전 요청
package com.javajava.project.domain.point.dto;

import lombok.Getter;

@Getter
public class ChargeRequestDto {
    //요청된 충전 금액
    private Long amount; //(원) 단위
}
