package com.javajava.project.global.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {
    // 기본 타임아웃 1시간
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    // 스레드 안전한 컬렉션으로 다수의 클라이언트 연결을 관리
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 클라이언트 SSE 연결 구독
     */
    public SseEmitter subscribe(String clientId) {
        // 기존 연결이 있으면 맵에서만 제거 (complete() 호출 시 브라우저 자동재연결 루프 발생하므로 금지)
        emitterMap.remove(clientId);

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterMap.put(clientId, emitter);

        emitter.onCompletion(() -> emitterMap.remove(clientId, emitter));
        emitter.onTimeout(() -> emitterMap.remove(clientId, emitter));
        emitter.onError((e) -> emitterMap.remove(clientId, emitter));

        // 503 Service Unavailable 방지용 첫 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE Connected"));
        } catch (IOException e) {
            emitterMap.remove(clientId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 특정 클라이언트에게 실시간 데이터 전송
     */
    public void sendToClient(String clientId, Object data) {
        SseEmitter emitter = emitterMap.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(data));
            } catch (IOException e) {
                emitterMap.remove(clientId, emitter);
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * 특정 사용자에게 포인트 갱신 이벤트 전송
     */
    public void sendPointUpdate(Long memberNo, Long currentPoints) {
        String clientId = String.valueOf(memberNo);
        SseEmitter emitter = emitterMap.get(clientId);
        if (emitter != null) {
            try {
                Map<String, Object> data = Map.of("points", currentPoints);
                emitter.send(SseEmitter.event().name("pointUpdate").data(data));
            } catch (IOException e) {
                emitterMap.remove(clientId, emitter);
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * 특정 사용자에게 강제 로그아웃 이벤트 전송
     */
    public void sendForceLogout(Long memberNo) {
        String clientId = String.valueOf(memberNo);
        SseEmitter emitter = emitterMap.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("forceLogout").data("{}"));
            } catch (IOException e) {
                emitterMap.remove(clientId, emitter);
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * 모든 연결된 클라이언트에게 실시간 입찰가 갱신 브로드캐스트
     */
    public void broadcastPriceUpdate(Long productNo, Long currentPrice) {
        Map<String, Object> data = Map.of("productNo", productNo, "currentPrice", currentPrice);
        List<String> deadClients = new ArrayList<>();

        emitterMap.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("priceUpdate").data(data));
            } catch (IOException e) {
                deadClients.add(clientId);
                emitter.completeWithError(e);
            }
        });

        deadClients.forEach(emitterMap::remove);
    }
}
