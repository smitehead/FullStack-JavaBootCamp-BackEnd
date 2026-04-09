package com.javajava.project.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PasswordChangeDto {
    @NotBlank private String currentPassword;
    @NotBlank @Size(min = 8, max = 20) private String newPassword;
}
