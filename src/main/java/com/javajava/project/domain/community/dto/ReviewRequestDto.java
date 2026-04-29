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

    private List<Long> tagIds; // 선택한 태그 ID 목록 (REVIEW_TAG_DEF.TAG_ID)

    private String content; // 후기 내용 (선택)
}
