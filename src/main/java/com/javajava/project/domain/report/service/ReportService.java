package com.javajava.project.domain.report.service;

import com.javajava.project.domain.report.dto.ReportRequestDto;
import com.javajava.project.domain.report.dto.ReportResponseDto;

import java.util.List;

public interface ReportService {

    // 신고 제출 (일반 사용자)
    Long submitReport(ReportRequestDto dto);

    // 전체 신고 목록
    List<ReportResponseDto> getAllReports();

    // 상태별 신고 목록
    List<ReportResponseDto> getReportsByStatus(String status);

    // 신고 처리 (상태 변경 + 제재 + 알림)
    void resolveReport(Long reportNo, String status, String penaltyMsg, Long adminNo);
}
