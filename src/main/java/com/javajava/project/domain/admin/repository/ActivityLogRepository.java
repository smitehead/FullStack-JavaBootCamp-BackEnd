package com.javajava.project.domain.admin.repository;

import com.javajava.project.domain.admin.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // 특정 관리자의 활동 로그 조회 (최신순)
    List<ActivityLog> findByAdminNoOrderByCreatedAtDesc(Long adminNo);

    // 대상 유형별 로그 조회 (예: "user", "product")
    List<ActivityLog> findByTargetTypeOrderByCreatedAtDesc(String targetType);

    // 특정 대상 ID + 유형으로 로그 조회
    List<ActivityLog> findByTargetIdAndTargetTypeOrderByCreatedAtDesc(Long targetId, String targetType);

    // 기간별 전체 로그 조회 (관리자 대시보드용)
    @Query("SELECT a FROM ActivityLog a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<ActivityLog> findByCreatedAtBetween(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
}
