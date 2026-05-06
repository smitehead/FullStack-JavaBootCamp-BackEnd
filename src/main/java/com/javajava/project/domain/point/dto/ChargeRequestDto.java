package com.javajava.project.domain.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChargeRequestDto {
    @NotNull(message = "충전 금액은 필수입니다.")
    @Min(value = 1000, message = "최소 충전 금액은 1,000원입니다.")
    private Long amount;
}
