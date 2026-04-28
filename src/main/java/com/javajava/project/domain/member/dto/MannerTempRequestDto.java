package com.javajava.project.domain.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MannerTempRequestDto {

    @NotNull(message = "변경할 매너온도는 필수입니다.")
    @DecimalMin(value = "0.0", message = "매너온도는 0도 이상이어야 합니다.")
    @DecimalMax(value = "100.0", message = "매너온도는 100도 이하이어야 합니다.")
    private Double newTemp;

    @NotBlank(message = "변경 사유는 필수입니다.")
    private String reason;
}
