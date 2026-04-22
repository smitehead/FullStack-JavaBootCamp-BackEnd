package com.javajava.project.domain.platform.repository;

import com.javajava.project.domain.platform.entity.PlatformRevenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PlatformRevenueRepository
        extends JpaRepository<PlatformRevenue, Long>,
                JpaSpecificationExecutor<PlatformRevenue> {

    // 전체 누적 수익 합계
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM PlatformRevenue r")
    Long sumTotalRevenue();

    // 특정 기간 수익 합계 (당월, 금일 등에 재사용)
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM PlatformRevenue r " +
           "WHERE r.createdAt >= :start AND r.createdAt < :end")
    Long sumRevenueInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
