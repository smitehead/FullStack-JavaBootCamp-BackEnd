package com.javajava.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductListResponseDto {
    private Long id;                  // 상품 번호 (프론트엔드의 id)
    private String title;             // 상품명
    private String location;          // 거래 위치
    private Long currentPrice;        // 현재 입찰가
    private LocalDateTime endTime;    // 경매 종료 시간
    private Long participantCount; // 참여자 수 (bidCount 매핑)
    
    private List<String> images;      // 썸네일 이미지 배열 (프론트엔드 images[0] 사용)
    
    private String status;            // 경매 상태 ("active" 또는 "completed")

    private Boolean isWishlisted;     // 로그인한 회원의 찜 여부

    private String bidStatus;         // 입찰 상태 ("bidding"=경매중, "won"=낙찰, "lost"=낙찰실패, null=해당없음)
}