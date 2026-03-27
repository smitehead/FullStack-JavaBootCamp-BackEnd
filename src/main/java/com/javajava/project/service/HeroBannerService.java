package com.javajava.project.service;

import com.javajava.project.dto.HeroBannerRequestDto;
import com.javajava.project.dto.HeroBannerResponseDto;
import java.util.List;

public interface HeroBannerService {

    // 활성 배너 목록 조회 (프론트용, 타입 미지정 시 전체)
    List<HeroBannerResponseDto> getActiveBanners(String type);

    // 전체 배너 목록 조회 (관리자용)
    List<HeroBannerResponseDto> getAllBanners();

    // 배너 등록 (관리자)
    Long create(HeroBannerRequestDto dto);

    // 배너 수정 (관리자)
    void update(Long bannerNo, HeroBannerRequestDto dto);

    // 배너 삭제 (관리자)
    void delete(Long bannerNo);

    // 배너 활성화/비활성화 토글 (관리자)
    void toggleActive(Long bannerNo);
}
