package com.javajava.project.repository;

import com.javajava.project.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 전체 신고 목록 (최신순)
    List<Report> findAllByOrderByCreatedAtDesc();

    // 상태별 신고 목록 (최신순)
    List<Report> findByStatusOrderByCreatedAtDesc(String status);

    // 특정 회원이 받은 신고 목록
    List<Report> findByTargetMemberNoOrderByCreatedAtDesc(Long targetMemberNo);

    // 특정 상품에 대한 신고 목록
    List<Report> findByTargetProductNoOrderByCreatedAtDesc(Long targetProductNo);
}
