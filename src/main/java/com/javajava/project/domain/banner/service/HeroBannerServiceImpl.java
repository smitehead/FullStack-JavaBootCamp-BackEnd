package com.javajava.project.domain.banner.service;

import com.javajava.project.domain.banner.dto.HeroBannerRequestDto;
import com.javajava.project.domain.banner.dto.HeroBannerResponseDto;
import com.javajava.project.domain.banner.entity.HeroBanner;
import com.javajava.project.domain.banner.repository.HeroBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HeroBannerServiceImpl implements HeroBannerService {

    private final HeroBannerRepository heroBannerRepository;

    /**
     * 활성 배너 목록 조회 (프론트 메인 홈용)
     * isActive=1이고 endAt이 null이거나 현재 시각 이후인 배너만 반환
     * sortOrder 오름차순 정렬
     */
    @Override
    public List<HeroBannerResponseDto> getActiveBanners(String type) {
        List<HeroBanner> banners = (type != null && !type.isBlank())
                ? heroBannerRepository.findActiveBannersByType(type)
                : heroBannerRepository.findActiveBanners();
        return banners.stream()
                .map(HeroBannerResponseDto::from)
                .toList();
    }

    /**
     * 배너 등록 (관리자)
     * sortOrder, isActive 미입력 시 엔티티 기본값(0, 1) 적용
     */
    /**
     * 전체 배너 목록 조회 (관리자용)
     * 활성/비활성 모두 포함, sortOrder 오름차순
     */
    @Override
    public List<HeroBannerResponseDto> getAllBanners() {
        return heroBannerRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(HeroBannerResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public Long create(HeroBannerRequestDto dto) {
        HeroBanner banner = HeroBanner.builder()
                .bannerType(dto.getBannerType() != null ? dto.getBannerType() : "hero")
                .imgUrl(dto.getImgUrl())
                .linkUrl(dto.getLinkUrl())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : 1)
                .endAt(dto.getEndAt())
                .build();
        return heroBannerRepository.save(banner).getBannerNo();
    }

    /**
     * 배너 수정 (관리자)
     * 존재하지 않는 배너 번호면 예외 발생
     */
    @Override
    @Transactional
    public void update(Long bannerNo, HeroBannerRequestDto dto) {
        HeroBanner banner = heroBannerRepository.findById(bannerNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너입니다."));

        if (dto.getBannerType() != null) banner.setBannerType(dto.getBannerType());
        banner.setImgUrl(dto.getImgUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        if (dto.getSortOrder() != null) banner.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive() != null) banner.setIsActive(dto.getIsActive());
        banner.setEndAt(dto.getEndAt());
    }

    /**
     * 배너 활성화/비활성화 토글 (관리자)
     */
    @Override
    @Transactional
    public void toggleActive(Long bannerNo) {
        HeroBanner banner = heroBannerRepository.findById(bannerNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너입니다."));
        banner.setIsActive(banner.getIsActive() == 1 ? 0 : 1);
    }

    /**
     * 배너 삭제 (관리자)
     * 존재하지 않는 배너 번호면 예외 발생
     */
    @Override
    @Transactional
    public void delete(Long bannerNo) {
        if (!heroBannerRepository.existsById(bannerNo)) {
            throw new IllegalArgumentException("존재하지 않는 배너입니다.");
        }
        heroBannerRepository.deleteById(bannerNo);
    }
}
