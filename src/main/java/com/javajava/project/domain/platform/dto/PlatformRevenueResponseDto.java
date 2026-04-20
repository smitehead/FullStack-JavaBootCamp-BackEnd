package com.javajava.project.domain.platform.dto;

import com.javajava.project.domain.platform.entity.PlatformRevenue;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PlatformRevenueResponseDto {

    private Long revenueNo;
    private Long amount;
    private String reason;
    private Long sourceMemberNo;
    private Long relatedProductNo;
    private LocalDateTime createdAt;

    public static PlatformRevenueResponseDto from(PlatformRevenue entity) {
        return PlatformRevenueResponseDto.builder()
                .revenueNo(entity.getRevenueNo())
                .amount(entity.getAmount())
                .reason(entity.getReason())
                .sourceMemberNo(entity.getSourceMemberNo())
                .relatedProductNo(entity.getRelatedProductNo())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
