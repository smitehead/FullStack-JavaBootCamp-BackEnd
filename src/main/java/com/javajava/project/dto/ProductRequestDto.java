package com.javajava.project.dto;

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
    private Long tradeEmdNo;
    private String tradeAddrDetail;
    private Long startPrice;
    private Long buyoutPrice;
    private Long minBidUnit;
    private LocalDateTime endTime;
}