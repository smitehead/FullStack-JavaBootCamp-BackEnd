package com.javajava.project.domain.report.repository;

import com.javajava.project.domain.report.entity.ReportImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {
    List<ReportImage> findByReportNo(Long reportNo);
}
