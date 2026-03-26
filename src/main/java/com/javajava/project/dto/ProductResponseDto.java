package com.javajava.project.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {
    private Long productNo;
    private String title;           // 상품 이름
    private String mainImageUrl;    // 상품 사진
    private String location;        // 주소 (tradeAddrDetail 활용)
    private Long currentPrice;      // 현재 최고가
    private LocalDateTime endTime;  // 남은 시간 계산을 위한 종료 시각

    /**
     * 경매 진행 상태
     * 0 : active (진행중)
     * 1 : completed (낙찰 완료)
     * 2 : canceled (취소)
     */
    private Integer status;         // [수정] isActive → status
}