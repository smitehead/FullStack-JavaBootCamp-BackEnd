package com.javajava.project.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeRequestDto {

    @NotBlank(message = "카테고리는 필수입니다.")
    @Size(max = 20)
    private String category; // 업데이트, 이벤트, 점검, 정책

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private Boolean isImportant; // true면 중요 공지

    private LocalDateTime maintenanceStart; // 점검 시작 일시
    private LocalDateTime maintenanceEnd;   // 점검 종료 일시
}
