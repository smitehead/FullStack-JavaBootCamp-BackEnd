package com.javajava.project.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidRequestDto {
    private Long productNo;  // 입찰 대상 상품
    private Long memberNo;   // 입찰자 (현재 로그인한 사용자)
    private Long bidPrice;   // 입찰 시도 금액
}