package com.javajava.project.domain.admin.controller;

import com.javajava.project.domain.admin.service.AdminRevenueService;
import com.javajava.project.domain.platform.dto.PlatformRevenueResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/revenue")
@RequiredArgsConstructor
public class AdminRevenueController {

    private final AdminRevenueService adminRevenueService;

    /**
     * 수익 통계 조회
     * GET /api/admin/revenue/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminRevenueService.getRevenueStats());
    }

    /**
     * 수익 내역 목록 조회
     * GET /api/admin/revenue?reason=&sourceMemberNo=&relatedProductNo=&startDate=&endDate=&page=1&size=20
     */
    @GetMapping
    public ResponseEntity<Page<PlatformRevenueResponseDto>> getRevenueList(
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long sourceMemberNo,
            @RequestParam(required = false) Long relatedProductNo,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(adminRevenueService.getRevenueList(
                reason, sourceMemberNo, relatedProductNo, startDate, endDate, page, size));
    }
}
