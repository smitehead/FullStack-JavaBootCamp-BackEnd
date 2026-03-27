package com.javajava.project.dto;

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
    private Integer status; // 0=active, 1=completed, 2=canceled
}
