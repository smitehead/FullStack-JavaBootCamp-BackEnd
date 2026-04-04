package com.javajava.project.domain.report.controller;

import com.javajava.project.domain.report.dto.ReportRequestDto;
import com.javajava.project.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 일반 사용자 신고 API
 * POST /api/reports - 신고 접수
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 신고 제출
     * Body: { reporterNo, targetMemberNo?, targetProductNo?, type, content? }
     */
    @PostMapping
    public ResponseEntity<Map<String, Long>> submitReport(
            @Valid @RequestBody ReportRequestDto dto) {
        Long reportNo = reportService.submitReport(dto);
        return ResponseEntity.ok(Map.of("reportNo", reportNo));
    }
}
