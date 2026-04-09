package com.javajava.project.global.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 기존 연결 교체 — 이전 emitter는 완료 처리 (정상 종료 → 브라우저가 새 연결 사용)
        SseEmitter old = emitterMap.put(clientId, emitter);
        if (old != null) {
            try { old.complete(); } catch (Exception ignored) {}
        }

        // 수명주기 콜백: emitter 가 만료/완료/에러 시 맵에서 제거
        emitter.onCompletion(() -> emitterMap.remove(clientId, emitter));
        emitter.onTimeout(() -> {
            emitterMap.remove(clientId, emitter);
            emitter.complete();
        });
        emitter.onError((e) -> emitterMap.remove(clientId, emitter));

        // 최초 연결 확인용 이벤트 (503 방지 + 프론트 재연결 감지용)
        sendSafe(clientId, emitter, SseEmitter.event().name("connect").data("SSE Connected"));

        return emitter;
    }

    /**
     * 스레드 안전 전송 헬퍼.
     * SseEmitter.send()는 스레드 비안전 → synchronized(emitter)로 직렬화.
     * 전송 실패 시 에미터를 조용히 제거하고 false 반환 (completeWithError 금지 — 브라우저 에러 재연결 루프 방지).
     */
    private boolean sendSafe(String clientId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        synchronized (emitter) {
            try {
                emitter.send(event);
                return true;
            } catch (Exception e) {
                // 전송 실패 → 맵에서 제거 후 정상 종료 (브라우저는 자동 재연결)
                emitterMap.remove(clientId, emitter);
                try { emitter.complete(); } catch (Exception ignored) {}
                return false;
            }
        }
    }

    /**
     * 특정 클라이언트에게 실시간 데이터 전송
     */
    public void sendToClient(String clientId, Object data) {
        SseEmitter emitter = emitterMap.get(clientId);
        if (emitter != null) {
            sendSafe(clientId, emitter, SseEmitter.event().name("notification").data(data));
        }
    }

    /**
     * 특정 사용자에게 포인트 갱신 이벤트 전송
     */
    public void sendPointUpdate(Long memberNo, Long currentPoints) {
        String clientId = String.valueOf(memberNo);
        SseEmitter emitter = emitterMap.get(clientId);
        if (emitter != null) {
            sendSafe(clientId, emitter,
                SseEmitter.event().name("pointUpdate").data(Map.of("points", currentPoints)));
        }
    }

    /**
     * 특정 사용자에게 강제 로그아웃 이벤트 전송
     */
    public void sendForceLogout(Long memberNo) {
        String clientId = String.valueOf(memberNo);
        SseEmitter emitter = emitterMap.get(clientId);
        if (emitter != null) {
            sendSafe(clientId, emitter, SseEmitter.event().name("forceLogout").data("{}"));
        }
    }

    /**
     * 모든 연결된 클라이언트에게 실시간 입찰가 갱신 브로드캐스트.
     * 각 emitter 별로 synchronized → 동시 입찰 시 동일 emitter에 concurrent send 방지.
     */
    public void broadcastPriceUpdate(Long productNo, Long currentPrice, Long bidderNo) {
        Map<String, Object> data = Map.of(
            "productNo", productNo,
            "currentPrice", currentPrice,
            "bidderNo", bidderNo
        );
        emitterMap.forEach((clientId, emitter) ->
            sendSafe(clientId, emitter, SseEmitter.event().name("priceUpdate").data(data))
        );
    }
}
