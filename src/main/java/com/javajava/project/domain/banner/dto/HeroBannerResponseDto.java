package com.javajava.project.domain.banner.dto;

import com.javajava.project.domain.banner.entity.HeroBanner;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroBannerResponseDto {

    private Long bannerNo;
    private String bannerType;
    private String imgUrl;
    private String linkUrl;
    private Integer sortOrder;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime endAt;

    // HeroBanner 엔티티 → ResponseDto 변환
    public static HeroBannerResponseDto from(HeroBanner banner) {
        return HeroBannerResponseDto.builder()
                .bannerNo(banner.getBannerNo())
                .bannerType(banner.getBannerType())
                .imgUrl(banner.getImgUrl())
                .linkUrl(banner.getLinkUrl())
                .sortOrder(banner.getSortOrder())
                .isActive(banner.getIsActive())
                .createdAt(banner.getCreatedAt())
                .endAt(banner.getEndAt())
                .build();
    }
}
