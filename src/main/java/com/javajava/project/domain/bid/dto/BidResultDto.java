package com.javajava.project.domain.bid.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResultDto {
    /** 자동입찰이 발동되어 수동 입찰자가 바로 밀려났는지 여부 */
    private boolean autoBidFired;
    /** 자동입찰 발동 후 실제 최고 입찰자 memberNo */
    private Long finalBidderNo;
    /** 자동입찰 발동 후 실제 최고 입찰가 */
    private Long finalPrice;
}
