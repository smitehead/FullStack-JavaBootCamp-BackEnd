package com.javajava.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MannerTempRequestDto {

    @NotNull(message = "변경할 매너온도는 필수입니다.")
    private Double newTemp;

    @NotBlank(message = "변경 사유는 필수입니다.")
    private String reason;
}
