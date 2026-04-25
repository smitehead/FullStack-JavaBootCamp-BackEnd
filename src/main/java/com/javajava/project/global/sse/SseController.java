package com.javajava.project.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SseController {

    private final SseService sseService;
    private final SseTicketService sseTicketService;

    /**
     * SSE 연결용 일회용 티켓 발급.
     * JWT 인증 필요 (Authorization 헤더) → 10초 유효 ticket 반환.
     */
    @PostMapping("/ticket")
    public ResponseEntity<Map<String, String>> issueTicket(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        Long memberNo = (Long) authentication.getPrincipal();
        String ticket = sseTicketService.generateTicket(memberNo);
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }

    /**
     * SSE 구독 엔드포인트.
     * - 로그인 사용자: ?ticket=<일회용 티켓> → memberNo를 clientId로 사용
     * - 비로그인(공개 페이지): Authentication 기반 UUID 사용
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(required = false) String ticket,
                                Authentication authentication) {
        String clientId;

        if (ticket != null) {
            Long memberNo = sseTicketService.validateAndConsume(ticket);
            clientId = (memberNo != null) ? String.valueOf(memberNo) : UUID.randomUUID().toString();
        } else {
            clientId = (authentication != null && authentication.isAuthenticated())
                    ? String.valueOf(authentication.getPrincipal())
                    : UUID.randomUUID().toString();
        }

        return sseService.subscribe(clientId);
    }
}
