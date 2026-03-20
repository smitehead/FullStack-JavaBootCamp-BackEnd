package com.javajava.project.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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
        sendToClient(clientId, "SSE Connected");
        
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
                emitterMap.remove(clientId);
                // 클라이언트 연결이 끊어진 경우 무시하거나 로그 처리
            }
        }
    }

    /**
     * 모든 연결된 클라이언트에게 실시간 입찰가 갱신 브로드캐스트
     */
    public void broadcastPriceUpdate(Long productNo, Long currentPrice) {
        Map<String, Object> data = Map.of("productNo", productNo, "currentPrice", currentPrice);
        
        // 현재 연결된 모든 사용자에게 'priceUpdate'라는 이벤트 이름으로 데이터 발송
        emitterMap.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("priceUpdate").data(data));
            } catch (IOException e) {
                emitterMap.remove(clientId);
            }
        });
    }
}
