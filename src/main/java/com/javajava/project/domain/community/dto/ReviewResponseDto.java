package com.javajava.project.domain.community.dto;

import com.javajava.project.domain.community.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class ReviewResponseDto {

    private Long reviewNo;
    private Long resultNo;
    private Long writerNo;
    private String writerNickname;
    private Long targetNo;
    private Integer rating;
    private List<String> tags;
    private String content;
    private LocalDateTime createdAt;

    public static ReviewResponseDto from(Review review, String writerNickname) {
        List<String> tagList = (review.getTags() != null && !review.getTags().isBlank())
                ? Arrays.asList(review.getTags().split(","))
                : Collections.emptyList();

        return ReviewResponseDto.builder()
                .reviewNo(review.getReviewNo())
                .resultNo(review.getResultNo())
                .writerNo(review.getWriterNo())
                .writerNickname(writerNickname)
                .targetNo(review.getTargetNo())
                .rating(review.getRating())
                .tags(tagList)
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
