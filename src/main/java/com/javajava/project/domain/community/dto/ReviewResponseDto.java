package com.javajava.project.domain.community.dto;

import com.javajava.project.domain.community.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewResponseDto {

    private Long reviewNo;
    private Long resultNo;
    private Long writerNo;
    private String writerNickname;
    private Long targetNo;
    private String writerRole;
    private Long productNo;
    private String productTitle;
    private List<String> tags;
    private String content;
    private LocalDateTime createdAt;

    public static ReviewResponseDto from(Review review, String writerNickname,
                                         Long productNo, String productTitle,
                                         List<String> tagNames) {
        return ReviewResponseDto.builder()
                .reviewNo(review.getReviewNo())
                .resultNo(review.getResultNo())
                .writerNo(review.getWriterNo())
                .writerNickname(writerNickname)
                .targetNo(review.getTargetNo())
                .writerRole(review.getWriterRole())
                .productNo(productNo)
                .productTitle(productTitle)
                .tags(tagNames)
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
