package com.javajava.project.domain.admin.controller;

import com.javajava.project.domain.notification.dto.NotificationResponseDto;
import com.javajava.project.domain.admin.entity.ActivityLog;
import com.javajava.project.domain.admin.repository.ActivityLogRepository;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 알림 관리 API
 * - 전체 회원에게 알림 발송
 * - 최근 발송 알림 조회
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;
    private final ActivityLogRepository activityLogRepository;

    /**
     * 전체 회원에게 알림 발송 (관리자용)
     * POST /api/admin/notifications/broadcast
     * Body: { "type": "시스템", "content": "...", "linkUrl": "/..." }
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcastNotification(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long adminNo = getAdminNo(authentication);
        String type = body.getOrDefault("type", "시스템");
        String content = body.get("content");
        String linkUrl = body.get("linkUrl");

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("알림 내용은 필수입니다.");
        }

        // 전체 활성 회원에게 알림 발송
        List<Long> memberNos = memberRepository.findAll().stream()
                .filter(m -> m.getIsActive() == 1)
                .map(m -> m.getMemberNo())
                .toList();

        for (Long memberNo : memberNos) {
            notificationService.sendAndSaveNotification(memberNo, type, content, linkUrl);
        }

        // 활동 로그 기록
        activityLogRepository.save(ActivityLog.builder()
                .adminNo(adminNo)
                .action("알림 발송")
                .targetType("notice")
                .details("전체 알림 발송: " + content)
                .build());

        return ResponseEntity.ok().build();
    }

    /**
     * 관리자 발송 알림 목록 조회 (중복 제거, 최근 50건)
     * GET /api/admin/notifications/recent?type=시스템
     * type 미입력 시 전체 관리자 타입(시스템/활동/입찰) 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationResponseDto>> getRecentNotifications(
            @RequestParam(required = false) String type) {
        List<NotificationResponseDto> result = notificationService.getAdminBroadcasts(type, 50);
        return ResponseEntity.ok(result);
    }

    private Long getAdminNo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return (Long) authentication.getPrincipal();
    }
}
