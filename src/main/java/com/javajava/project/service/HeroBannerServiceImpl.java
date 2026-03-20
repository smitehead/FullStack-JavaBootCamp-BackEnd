package com.javajava.project.service;

import com.javajava.project.dto.HeroBannerRequestDto;
import com.javajava.project.dto.HeroBannerResponseDto;
import com.javajava.project.entity.HeroBanner;
import com.javajava.project.repository.HeroBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

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
    public List<HeroBannerResponseDto> getActiveBanners() {
        return heroBannerRepository.findActiveBanners()
                .stream()
                .map(HeroBannerResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 배너 등록 (관리자)
     * sortOrder, isActive 미입력 시 엔티티 기본값(0, 1) 적용
     */
    @Override
    @Transactional
    public Long create(HeroBannerRequestDto dto) {
        HeroBanner banner = HeroBanner.builder()
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

        banner.setImgUrl(dto.getImgUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        if (dto.getSortOrder() != null) banner.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive() != null) banner.setIsActive(dto.getIsActive());
        banner.setEndAt(dto.getEndAt());
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
