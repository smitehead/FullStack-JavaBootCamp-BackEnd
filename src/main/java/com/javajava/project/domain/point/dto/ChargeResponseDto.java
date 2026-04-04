//충전 결과 응답
package com.javajava.project.domain.point.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChargeResponseDto {
    private boolean success;
    //충전 후 포인트 잔액
    private Long newBalance;
    private String message;
}
