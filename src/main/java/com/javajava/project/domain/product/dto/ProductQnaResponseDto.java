package com.javajava.project.domain.product.dto;

import com.javajava.project.domain.product.entity.ProductQna;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class ProductQnaResponseDto {

    private Long qnaNo;
    private Long productNo;
    private Long memberNo;
    private String memberNickname;
    private String content;
    private String answer;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;

    public static ProductQnaResponseDto from(ProductQna qna, String memberNickname) {
        return ProductQnaResponseDto.builder()
                .qnaNo(qna.getPrdtQnaNo())
                .productNo(qna.getProductNo())
                .memberNo(qna.getMemberNo())
                .memberNickname(memberNickname)
                .content(qna.getContent())
                .answer(qna.getAnswer())
                .answeredAt(qna.getAnsweredAt())
                .createdAt(qna.getCreatedAt())
                .build();
    }
}
