package com.javajava.project.domain.community.dto;

import com.javajava.project.domain.community.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponseDto {

    private Long reviewNo;
    private Long resultNo;
    private Long writerNo;
    private String writerNickname;
    private Long targetNo;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;

    public static ReviewResponseDto from(Review review, String writerNickname) {
        return ReviewResponseDto.builder()
                .reviewNo(review.getReviewNo())
                .resultNo(review.getResultNo())
                .writerNo(review.getWriterNo())
                .writerNickname(writerNickname)
                .targetNo(review.getTargetNo())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
