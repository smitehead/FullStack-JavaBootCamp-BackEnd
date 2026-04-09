package com.javajava.project.domain.product.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
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
    private LocalDateTime endTime;
    private Long participantCount;

    private List<String> images;
    private Boolean isWishlisted;
    private Long wishlistCount;

    private SellerInfoDto seller;
    private List<BidHistoryDto> bidHistory;

    @Getter @Setter @Builder
    public static class SellerInfoDto {
        private Long sellerNo;
        private String nickname;
        private Double mannerTemp;
        private String profileImgUrl;
    }

    @Getter @Setter @Builder
    public static class BidHistoryDto {
        private String bidderNickname;
        private Long bidPrice;
        private LocalDateTime bidTime;
    }
}