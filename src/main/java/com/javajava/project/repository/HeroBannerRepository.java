package com.javajava.project.repository;

import com.javajava.project.entity.HeroBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface HeroBannerRepository extends JpaRepository<HeroBanner, Long> {

    // 활성 배너 목록 조회 (isActive=1, endAt이 null이거나 현재 시각 이후인 것만)
    // Oracle 11g 호환을 위해 CURRENT_TIMESTAMP 사용
    @Query("SELECT b FROM HeroBanner b WHERE b.isActive = 1 AND (b.endAt IS NULL OR b.endAt > CURRENT_TIMESTAMP) ORDER BY b.sortOrder ASC")
    List<HeroBanner> findActiveBanners();

    // 전체 배너 목록 조회 (관리자용, 활성/비활성 모두 포함)
    List<HeroBanner> findAllByOrderBySortOrderAsc();
}
