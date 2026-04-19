package com.javajava.project.domain.auction.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SellerAuctionResultResponseDto {

    private Long resultNo;
    private String status; // 배송대기 | 취소요청 | 결제완료 | 구매확정 | 거래취소

    private LocalDateTime confirmedAt;

    // 상품 정보
    private Long productNo;
    private String title;
    private String description;
    private Long finalPrice;         // 낙찰가 (판매자 수령 예정 기본액)
    private String tradeType;
    private String location;
    private List<String> images;

    // 낙찰자(구매자) 정보
    private BuyerInfo buyer;

    // 배송지 정보 (구매자가 입력한 주소)
    private String deliveryAddrRoad;
    private String deliveryAddrDetail;

    @Getter
    @Builder
    public static class BuyerInfo {
        private Long buyerNo;
        private String nickname;
        private Double mannerTemp;
        private String profileImage;
    }
}
