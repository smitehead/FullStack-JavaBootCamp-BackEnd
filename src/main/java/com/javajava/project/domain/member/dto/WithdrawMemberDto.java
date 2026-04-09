package com.javajava.project.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class WithdrawMemberDto {
    @NotBlank private String password;
    @NotBlank private String reason;
    private String reasonDetail;
}
