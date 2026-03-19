package com.javajava.project.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailResponseDto {
    // 상품 기본 정보
    private Long productNo;
    private String title;           // [cite: 138]
    private String description;     // [cite: 161]
    private String categoryName;    // [cite: 136]
    private String tradeType;       // [cite: 139]
    private String location;        // [cite: 140]
    private Long startPrice;        // [cite: 149]
    private Long currentPrice;      // [cite: 151]
    private Long minBidUnit;        // [cite: 150]
    private LocalDateTime startTime; // [cite: 125]
    private LocalDateTime endTime;   // [cite: 147]
    private Long participantCount;  // [cite: 148]
    
    // 이미지 리스트 [cite: 129]
    private List<String> imageUrls;

    // 판매자 정보 [cite: 143]
    private SellerInfoDto seller;

    // 입찰 기록 리스트 [cite: 168]
    private List<BidHistoryDto> bidHistory;

    // 로그인한 사용자의 찜 여부 [cite: 153]
    private Boolean isWishlisted;

    @Getter @Setter @Builder
    public static class SellerInfoDto {
        private Long sellerNo;
        private String nickname;     // [cite: 143]
        private String profileImageUrl;
        private Double mannerTemp;   // [cite: 144]
    }

    @Getter @Setter @Builder
    public static class BidHistoryDto {
        private String bidderNickname; // [cite: 171]
        private Long bidPrice;         // [cite: 172]
        private LocalDateTime bidTime; // [cite: 172]
    }
}
