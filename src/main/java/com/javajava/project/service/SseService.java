package com.javajava.project.service;

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
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterMap.put(clientId, emitter);

        // 연결이 종료되거나 타임아웃 시 맵에서 제거 (메모리 누수 방지)
        emitter.onCompletion(() -> emitterMap.remove(clientId));
        emitter.onTimeout(() -> emitterMap.remove(clientId));
        emitter.onError((e) -> emitterMap.remove(clientId));

        // 503 Service Unavailable 방지용 첫 더미 데이터 전송
        // "notification" 이벤트명을 쓰면 프론트가 JSON.parse 시도 → SyntaxError 발생
        // "connect" 이벤트명은 프론트에서 별도 처리 없이 무시됨
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
                // 클라이언트가 연결을 먼저 끊은 경우 (페이지 이동, 탭 닫기 등) — 정상 동작
                emitterMap.remove(clientId);
                emitter.completeWithError(e); // zombie 상태 방지
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
                emitterMap.remove(clientId);
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * 특정 사용자에게 강제 로그아웃 이벤트 전송 (다른 기기에서 로그인 감지 시)
     * SSE 연결이 없으면(오프라인 등) 건너뜀 → 이후 API 요청 시 401 인터셉터로 처리됨
     */
    public void sendForceLogout(Long memberNo) {
        String clientId = String.valueOf(memberNo);
        SseEmitter emitter = emitterMap.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("forceLogout").data("{}"));
            } catch (IOException e) {
                emitterMap.remove(clientId);
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * 모든 연결된 클라이언트에게 실시간 입찰가 갱신 브로드캐스트
     * 【수정】forEach 루프 중 직접 remove 대신, 실패한 ID를 수집하여 루프 후 일괄 제거
     *         → ConcurrentHashMap 반복 중 구조 변경으로 인한 예외 위험 제거
     */
    public void broadcastPriceUpdate(Long productNo, Long currentPrice) {
        Map<String, Object> data = Map.of("productNo", productNo, "currentPrice", currentPrice);

        // 전송 실패한 클라이언트 ID를 별도 리스트에 수집
        List<String> deadClients = new ArrayList<>();

        emitterMap.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("priceUpdate").data(data));
            } catch (IOException e) {
                // 루프 중에는 수집만 하고, 실제 제거는 루프 종료 후 진행
                deadClients.add(clientId);
                emitter.completeWithError(e); // 클라이언트 연결 명시적 종료
            }
        });

        // 루프 종료 후 끊어진 클라이언트 일괄 제거
        deadClients.forEach(emitterMap::remove);
    }
}
