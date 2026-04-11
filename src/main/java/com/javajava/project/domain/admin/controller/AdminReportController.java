package com.javajava.project.domain.admin.controller;

import com.javajava.project.domain.report.dto.ReportResolveRequestDto;
import com.javajava.project.domain.report.dto.ReportResponseDto;
import com.javajava.project.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 전용 신고 관리 API
 * - 신고 목록 조회 (전체/상태별)
 * - 신고 처리 (상태 변경 + 제재 + 알림)
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /**
     * 신고 목록 조회 (상태 필터 가능)
     * GET /api/admin/reports?status=접수
     */
    @GetMapping
    public ResponseEntity<List<ReportResponseDto>> getReports(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(reportService.getReportsByStatus(status));
        }
        return ResponseEntity.ok(reportService.getAllReports());
    }

    /**
     * 신고 단건 조회 (이미지 포함)
     * GET /api/admin/reports/{reportNo}
     */
    @GetMapping("/{reportNo}")
    public ResponseEntity<ReportResponseDto> getReportDetail(@PathVariable("reportNo") Long reportNo) {
        return ResponseEntity.ok(reportService.getReportDetail(reportNo));
    }

    /**
     * 신고 처리
     * PUT /api/admin/reports/{reportNo}/resolve
     */
    @PutMapping("/{reportNo}/resolve")
    public ResponseEntity<Void> resolveReport(
            @PathVariable("reportNo") Long reportNo,
            @Valid @RequestBody ReportResolveRequestDto dto,
            Authentication authentication) {
        Long adminNo = getAdminNo(authentication);
        reportService.resolveReport(reportNo, dto.getStatus(), dto.getPenaltyMsg(), adminNo);
        return ResponseEntity.ok().build();
    }

    private Long getAdminNo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return (Long) authentication.getPrincipal();
    }
}
