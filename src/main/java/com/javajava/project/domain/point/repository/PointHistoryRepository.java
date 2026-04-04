package com.javajava.project.domain.point.repository;

import com.javajava.project.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    // 회원의 포인트 변동 내역 페이징 최신순으로 조회
    Page<PointHistory> findByMemberNoOrderByCreatedAtDesc(Long memberNo, Pageable pageable);
}