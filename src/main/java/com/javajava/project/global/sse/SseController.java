package com.javajava.project.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SseController {

    private final SseService sseService;

    /**
     * SSE 구독 엔드포인트.
     * - 인증된 사용자: JWT에서 추출한 memberNo를 클라이언트 식별자로 사용 (IDOR 방지)
     * - 비인증 요청(공개 페이지 실시간 가격 업데이트 등): 랜덤 UUID 사용
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        String clientId = (authentication != null && authentication.isAuthenticated())
                ? String.valueOf(authentication.getPrincipal())
                : UUID.randomUUID().toString();

        return sseService.subscribe(clientId);
    }
}
