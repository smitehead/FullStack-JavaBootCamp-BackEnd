package com.javajava.project.domain.auction.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AuctionResultResponseDto {
    private Long resultNo; // 낙찰 결과 번호
    private String status; // 거래 상태 (배송대기, 구매확정 등)
    private LocalDateTime confirmedAt; // 구매 확정 시각

    // 상품 정보
    private Long productNo;
    private String title;
    private String description;
    private Long finalPrice; // 최종 낙찰가
    private String tradeType; // 거래 방식
    private String location; // 거래 위치
    private List<String> images; // 상품 이미지

    // 판매자 정보
    private SellerInfo seller;

    // 배송지 정보
    private String deliveryAddrRoad;
    private String deliveryAddrDetail;

    /**
     * 입찰 취소 위약금 풀에서 구매자에게 돌아오는 캐시백 (penaltyPool / 2).
     * 경쟁 입찰자 취소가 없었으면 0. 결제 완료 시 포인트로 지급됨.
     */
    private Long buyerCashback;

    /**
     * 강제 승계 여부. 1 = 상위 입찰자 취소로 자동 승계된 낙찰자.
     * 이 경우 불이익 없이 낙찰 취소 가능 (매너온도 패널티 면제).
     */
    private Integer isForcePromoted;

    @Getter
    @Builder
    public static class SellerInfo {
        private Long sellerNo;
        private String nickname;
        private Double mannerTemp;
        private String profileImage;
    }
}
