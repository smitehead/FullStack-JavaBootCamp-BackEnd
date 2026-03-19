package com.javajava.project.dto;

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

    // 현재 서비스에서 주석 처리된 부분
    // private List<String> imageUrls;
    // private Boolean isWishlisted;

    private SellerInfoDto seller;
    private List<BidHistoryDto> bidHistory;

    @Getter @Setter @Builder
    public static class SellerInfoDto {
        private Long sellerNo;
        private String nickname;
        private Double mannerTemp;
    }

    @Getter @Setter @Builder
    public static class BidHistoryDto {
        private String bidderNickname;
        private Long bidPrice;
        private LocalDateTime bidTime;
    }
}