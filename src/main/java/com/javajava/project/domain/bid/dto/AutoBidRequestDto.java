package com.javajava.project.domain.bid.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoBidRequestDto {
    private Long productNo;
    private Long maxPrice;   // 자동입찰 최대 금액
}
