package com.javajava.project.domain.point.dto;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class WithdrawResponseDto {
    private boolean success;
    private Long remainBalance;
    private String message;
}