package com.javajava.project.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoBidResponseDto {
    private Long autoBidNo;
    private Long memberNo;
    private Long productNo;
    private Long maxPrice;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
