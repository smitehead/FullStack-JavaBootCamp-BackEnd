package com.javajava.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsRequestDto {

    @NotNull(message = "포인트 증감량은 필수입니다.")
    private Long pointAmount; // 양수: 증가, 음수: 감소
}
