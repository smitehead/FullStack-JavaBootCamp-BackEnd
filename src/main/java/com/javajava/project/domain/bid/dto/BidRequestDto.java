package com.javajava.project.domain.bid.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidRequestDto {
    private Long productNo;   // 입찰 대상 상품
    private Long memberNo;    // 입찰자 (현재 로그인한 사용자)
    private Long bidPrice;    // 입찰 시도 금액
    private Long targetPrice; // 유저가 입찰 버튼 클릭 시점에 화면에서 본 현재가 (가격 변동 감지용)
}