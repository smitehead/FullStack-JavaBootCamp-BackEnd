package com.javajava.project.domain.banner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroBannerRequestDto {

    @Size(max = 10, message = "배너 타입은 10자 이하여야 합니다.")
    private String bannerType;  // 배너 타입 (hero, ad) 미입력 시 기본값 hero

    @NotBlank(message = "이미지 URL은 필수입니다.")
    @Size(max = 255, message = "이미지 URL은 255자 이하여야 합니다.")
    private String imgUrl;      // 배너 이미지 URL

    @Size(max = 255, message = "링크 URL은 255자 이하여야 합니다.")
    private String linkUrl;     // 클릭 시 이동할 URL (선택)

    private Integer sortOrder;  // 노출 순서 (미입력 시 기본값 0)
    private Integer isActive;   // 노출 여부 (미입력 시 기본값 1)
    private LocalDateTime endAt; // 노출 종료 일시 (null이면 계속 노출)
}
