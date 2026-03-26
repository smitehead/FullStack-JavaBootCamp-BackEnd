package com.javajava.project.repository;

import com.javajava.project.entity.MannerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MannerHistoryRepository extends JpaRepository<MannerHistory, Long> {

    // 특정 회원의 매너온도 변동 이력 조회 (최신순)
    List<MannerHistory> findByMemberNoOrderByCreatedAtDesc(Long memberNo);

    // 특정 관리자가 처리한 변동 이력 조회
    List<MannerHistory> findByAdminNoOrderByCreatedAtDesc(Long adminNo);

    // 자동 변동(거래 후기 기반) 이력 조회 - adminNo가 NULL인 경우
    List<MannerHistory> findByMemberNoAndAdminNoIsNull(Long memberNo);
}