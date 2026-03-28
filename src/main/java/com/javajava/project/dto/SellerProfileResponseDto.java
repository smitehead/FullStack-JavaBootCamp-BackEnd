package com.javajava.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SellerProfileResponseDto {
    private Long sellerNo;
    private String nickname;
    private String profileImgUrl;
    private Double mannerTemp;
    private LocalDateTime joinedAt;
    private List<ProductListResponseDto> products;
}
