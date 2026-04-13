package com.javajava.project.domain.product.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {
    private Long sellerNo;
    private Long categoryNo;
    private String title;
    private String description;
    private String tradeType;
    private String tradeAddrDetail;
    private String tradeAddrShort;
    private Long startPrice;
    private Long buyoutPrice;
    private Long minBidUnit;
    private LocalDateTime endTime;
    private Long shippingFee;
}