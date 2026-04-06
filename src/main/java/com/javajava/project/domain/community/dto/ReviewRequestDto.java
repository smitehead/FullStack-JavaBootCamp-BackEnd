package com.javajava.project.domain.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    private Integer rating; // 별점 (선택, 1~5)

    private List<String> tags; // 태그 목록 (선택)

    private String content; // 후기 내용 (선택)
}
