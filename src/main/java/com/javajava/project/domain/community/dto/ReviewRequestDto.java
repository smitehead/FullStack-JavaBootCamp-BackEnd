package com.javajava.project.domain.community.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReviewRequestDto {

    @NotNull(message = "낙찰 결과 번호는 필수입니다.")
    private Long resultNo;

    private List<String> tags; // 태그 목록 (선택)

    private String content; // 후기 내용 (선택)

    private String role; // 작성자 역할: "buyer" | "seller" (선택, 서버에서 재검증)
}
