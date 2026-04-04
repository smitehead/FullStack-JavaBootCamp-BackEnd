package com.javajava.project.domain.report.service;

import com.javajava.project.domain.report.dto.ReportResponseDto;

import java.util.List;

public interface ReportService {

    // 전체 신고 목록
    List<ReportResponseDto> getAllReports();

    // 상태별 신고 목록
    List<ReportResponseDto> getReportsByStatus(String status);

    // 신고 처리 (상태 변경 + 제재 내용 + 알림)
    void resolveReport(Long reportNo, String status, String penaltyMsg, Long adminNo);
}
