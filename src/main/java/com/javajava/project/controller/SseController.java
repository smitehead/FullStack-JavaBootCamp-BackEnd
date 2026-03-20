package com.javajava.project.controller;

import com.javajava.project.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    // 비로그인 사용자도 구독할 수 있도록 ID를 파라미터로 받거나 자동 생성합니다.
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(value = "clientId", required = false) String clientId) {
        // 전달된 ID가 없으면 비로그인 게스트로 간주하고 고유 UUID를 발급
        if (clientId == null || clientId.isEmpty()) {
            clientId = UUID.randomUUID().toString();
        }
        
        return sseService.subscribe(clientId);
    }
}
