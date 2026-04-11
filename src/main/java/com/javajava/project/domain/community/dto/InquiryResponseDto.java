package com.javajava.project.domain.community.dto;

import com.javajava.project.domain.community.entity.Inquiry;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
public class InquiryResponseDto {
    private Long inquiryNo;
    private Long memberNo;
    private String memberNickname;
    private String type;
    private String bugType;
    private String title;
    private String content;
    private Integer status;
    private String answer;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;
    private Long adminNo;
    private String adminNickname;
    private List<String> imageUrls; // 첨부 이미지 URL 목록

    public static InquiryResponseDto from(Inquiry i, String nickname) {
        return InquiryResponseDto.builder()
                .inquiryNo(i.getInquiryNo())
                .memberNo(i.getMemberNo())
                .memberNickname(nickname)
                .type(i.getType())
                .bugType(i.getBugType())
                .title(i.getTitle())
                .content(i.getContent())
                .status(i.getStatus())
                .answer(i.getAnswer())
                .answeredAt(i.getAnsweredAt())
                .createdAt(i.getCreatedAt())
                .adminNo(i.getAdminNo())
                .adminNickname(i.getAdminNickname())
                .build();
    }
}