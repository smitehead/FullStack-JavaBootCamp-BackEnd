package com.javajava.project.repository;

import com.javajava.project.entity.HeroBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface HeroBannerRepository extends JpaRepository<HeroBanner, Long> {

    // 활성 배너 목록 조회 (전체 타입)
    @Query("SELECT b FROM HeroBanner b WHERE b.isActive = 1 AND (b.endAt IS NULL OR b.endAt > CURRENT_TIMESTAMP) ORDER BY b.sortOrder ASC")
    List<HeroBanner> findActiveBanners();

    // 활성 배너 목록 조회 (타입별)
    @Query("SELECT b FROM HeroBanner b WHERE b.isActive = 1 AND b.bannerType = :type AND (b.endAt IS NULL OR b.endAt > CURRENT_TIMESTAMP) ORDER BY b.sortOrder ASC")
    List<HeroBanner> findActiveBannersByType(String type);

    // 전체 배너 목록 조회 (관리자용, 활성/비활성 모두 포함)
    List<HeroBanner> findAllByOrderBySortOrderAsc();
}
