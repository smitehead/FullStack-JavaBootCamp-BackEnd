package com.javajava.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspendRequestDto {

    @NotNull(message = "정지 일수는 필수입니다.")
    @Min(value = 1, message = "정지 일수는 1일 이상이어야 합니다.")
    private Integer suspendDays; // 999 = 영구정지

    @NotBlank(message = "정지 사유는 필수입니다.")
    private String suspendReason;
}
