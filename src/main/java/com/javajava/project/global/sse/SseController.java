package com.javajava.project.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(value = "clientId", required = false) String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            clientId = UUID.randomUUID().toString();
        }

        return sseService.subscribe(clientId);
    }
}
