package com.javajava.project.domain.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResolveRequestDto {

    @NotBlank(message = "처리 상태는 필수입니다.")
    private String status; // 완료, 반려

    private String penaltyMsg; // 제재 내용 (선택)
}
