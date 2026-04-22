package com.javajava.project.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminProductResponseDto {
    private Long productNo;
    private String title;
    private String mainImageUrl;
    private String sellerNickname;
    private Long sellerNo;
    private Long startPrice;
    private Long currentPrice;
    private Long participantCount;
    private LocalDateTime endTime;
    private String status; // "active", "ended", "completed", "canceled"
    private Long categoryNo;
}
