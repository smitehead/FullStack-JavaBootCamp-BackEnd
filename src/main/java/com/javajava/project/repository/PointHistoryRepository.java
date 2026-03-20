package com.javajava.project.repository;

import com.javajava.project.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    // 특정 회원의 포인트 내역 조회 (최신순)
    List<PointHistory> findByMemberNoOrderByCreatedAtDesc(Long memberNo);
}