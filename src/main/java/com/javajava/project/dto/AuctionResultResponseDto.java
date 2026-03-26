package com.javajava.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AuctionResultResponseDto {
    private Long resultNo;           // 낙찰 결과 번호
    private String status;           // 거래 상태 (배송대기, 구매확정 등)
    private LocalDateTime confirmedAt; // 구매 확정 시각

    // 상품 정보
    private Long productNo;
    private String title;
    private String description;
    private Long finalPrice;          // 최종 낙찰가
    private String tradeType;         // 거래 방식
    private String location;          // 거래 위치
    private List<String> images;      // 상품 이미지

    // 판매자 정보
    private SellerInfo seller;

    // 배송지 정보
    private Long deliveryEmdNo;
    private String deliveryAddrDetail;

    @Getter
    @Builder
    public static class SellerInfo {
        private Long sellerNo;
        private String nickname;
        private Double mannerTemp;
        private String profileImage;
    }
}
