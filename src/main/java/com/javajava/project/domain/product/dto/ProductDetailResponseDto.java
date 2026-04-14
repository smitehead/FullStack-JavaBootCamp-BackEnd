package com.javajava.project.domain.product.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailResponseDto {
    private Long productNo;
    private String title;
    private String description;
    private String tradeType;
    private String location;
    private Long startPrice;
    private Long currentPrice;
    private Long minBidUnit;
    private LocalDateTime createdAt;
    private LocalDateTime endTime;
    private Long buyoutPrice;
    private Long shippingFee;
    private Long participantCount;

    /**
     * 경매 진행 상태.
     * 0: 진행중(ACTIVE) | 1: 낙찰완료(COMPLETED) | 2: 판매자취소(CANCELED)
     * 3: 결제대기(PENDING_PAYMENT) | 4: 유찰/최종실패(CLOSED_FAILED)
     */
    private Integer status;

    private List<String> images;
    private Boolean isWishlisted;
    private Long wishlistCount;

    private List<CategoryDto> categoryPath;
    private SellerInfoDto seller;
    private List<BidHistoryDto> bidHistory;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private Long id;
        private String name;
        private Integer depth;
    }

    @Getter
    @Setter
    @Builder
    public static class SellerInfoDto {
        private Long sellerNo;
        private String nickname;
        private Double mannerTemp;
        private String profileImgUrl;
    }

    @Getter
    @Setter
    @Builder
    public static class BidHistoryDto {
        private String bidderNickname;
        private Long bidPrice;
        private LocalDateTime bidTime;
    }
}