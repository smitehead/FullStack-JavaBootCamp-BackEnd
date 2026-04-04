package com.javajava.project.domain.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestDto {

    @NotNull(message = "신고자 회원번호는 필수입니다.")
    private Long reporterNo;

    /** 피신고자 회원번호 - 회원 신고 시에만 사용 */
    private Long targetMemberNo;

    /** 신고 대상 상품번호 - 상품 신고 시에만 사용 */
    private Long targetProductNo;

    @NotBlank(message = "신고 유형은 필수입니다.")
    private String type;   // DB TYPE 컬럼에 텍스트 그대로 저장

    private String content; // DB CONTENT 컬럼 (선택)
}
