package com.javajava.project.controller;

import com.javajava.project.dto.NotificationResponseDto;
import com.javajava.project.service.NotificationService;
import com.javajava.project.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;

    /**
     * 알림 목록 조회 (로그인 필요)
     * GET /api/notifications
     * Header: Authorization: Bearer {token}
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(Authentication authentication) {
        Long memberNo = getMemberNo(authentication);
        return ResponseEntity.ok(notificationService.getNotifications(memberNo));
    }

    /**
     * 미읽음 알림 개수 조회 (헤더 뱃지용)
     * GET /api/notifications/unread-count
     * Header: Authorization: Bearer {token}
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long memberNo = getMemberNo(authentication);
        long count = notificationService.getUnreadCount(memberNo);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 단일 알림 읽음 처리
     * PATCH /api/notifications/{notiNo}/read
     * Header: Authorization: Bearer {token}
     */
    @PatchMapping("/{notiNo}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notiNo) {
        notificationService.markAsRead(notiNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 전체 알림 읽음 처리
     * PATCH /api/notifications/read-all
     * Header: Authorization: Bearer {token}
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        Long memberNo = getMemberNo(authentication);
        notificationService.markAllAsRead(memberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 테스트용 — SSE 알림 수동 발송 (확인 후 제거)
     * POST /api/notifications/test-send/{memberNo}
     */
    @PostMapping("/test-send/{memberNo}")
    public ResponseEntity<Void> testSend(@PathVariable Long memberNo) {
        sseService.sendToClient(String.valueOf(memberNo), Map.of(
            "notiNo", 9999,
            "type", "bid",
            "content", "테스트 알림입니다! SSE 연동 성공 🎉",
            "linkUrl", "/",
            "isRead", 0,
            "createdAt", LocalDateTime.now().toString()
        ));
        return ResponseEntity.ok().build();
    }

    /**
     * SecurityContext에서 memberNo 추출
     * JwtAuthenticationFilter에서 principal에 memberNo(Long)를 저장해둠
     */
    private Long getMemberNo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return (Long) authentication.getPrincipal();
    }
}
