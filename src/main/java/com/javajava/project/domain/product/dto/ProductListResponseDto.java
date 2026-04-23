package com.javajava.project.domain.product.dto;

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

    private String auctionResultStatus; // 낙찰 결과 결제 상태 ("결제대기", "결제완료", "구매확정")

    private Long resultNo;            // 낙찰 결과 번호 (구매내역 후기 작성용)
    private Boolean hasReview;        // 후기 작성 여부 (구매내역용)
    private Boolean hasBuyerReview;   // 구매자가 작성한 후기 존재 여부
    private Boolean hasSellerReview;  // 판매자가 작성한 후기 존재 여부
    private Long winnerNo;            // 낙찰자 회원번호 (판매자용)
    private String winnerNickname;    // 낙찰자 닉네임 (판매자용)
}