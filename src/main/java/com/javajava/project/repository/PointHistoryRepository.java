package com.javajava.project.repository;

import com.javajava.project.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    // 추가적인 포인트 내역 조회 쿼리 메서드가 필요하다면 여기에 작성합니다.
}