package com.javajava.project.controller;

import com.javajava.project.dto.ActivityLogResponseDto;
import com.javajava.project.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 활동 로그 조회 API
 */
@RestController
@RequestMapping("/api/admin/activity-logs")
@RequiredArgsConstructor
public class AdminActivityLogController {

    private final AdminService adminService;

    /**
     * 활동 로그 조회 (전체 또는 대상 유형별)
     * GET /api/admin/activity-logs
     * GET /api/admin/activity-logs?targetType=user
     */
    @GetMapping
    public ResponseEntity<List<ActivityLogResponseDto>> getActivityLogs(
            @RequestParam(required = false) String targetType) {
        if (targetType != null && !targetType.isBlank()) {
            return ResponseEntity.ok(adminService.getActivityLogsByTargetType(targetType));
        }
        return ResponseEntity.ok(adminService.getAllActivityLogs());
    }
}
