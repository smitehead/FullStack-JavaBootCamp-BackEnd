package com.javajava.project.domain.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javajava.project.domain.report.dto.ReportRequestDto;
import com.javajava.project.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 일반 사용자 신고 API
 * POST /api/reports - 신고 접수 (이미지 첨부 포함)
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    /**
     * 신고 제출 (multipart/form-data)
     * Part "data"  : JSON { reporterNo, targetMemberNo?, targetProductNo?, type, content? }
     * Part "images": 이미지 파일 (선택, 최대 5개)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Long>> submitReport(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {
        ReportRequestDto dto = objectMapper.readValue(dataJson, ReportRequestDto.class);
        Long reportNo = reportService.submitReport(dto, images);
        return ResponseEntity.ok(Map.of("reportNo", reportNo));
    }
}
