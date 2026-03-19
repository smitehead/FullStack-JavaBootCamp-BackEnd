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
    private String mainImageUrl;    // 상품 사진 (Image 엔티티 연동 필요)
    private String location;        // 주소 (tradeAddrDetail 활용)
    private Long currentPrice;      // 현재 최고가
    private LocalDateTime endTime;  // 남은 시간 계산을 위한 종료 시각
    private Integer isActive;       // 경매 종료 여부 상태값
}