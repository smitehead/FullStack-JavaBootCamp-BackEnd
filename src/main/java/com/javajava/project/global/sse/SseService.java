package com.javajava.project.global.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
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
            // memberNo를 payload에 포함 → 프론트가 "내 이벤트인지" 2차 검증 가능
            sendSafe(clientId, emitter,
                SseEmitter.event().name("pointUpdate").data(
                    Map.of("points", currentPoints, "memberNo", memberNo)));
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

    /**
     * 즉시구매 또는 입찰가 도달로 인한 경매 종료 브로드캐스트.
     * priceUpdate 이벤트에 auctionEnded=true 포함 → 프론트가 경매 종료로 인식.
     */
    public void broadcastBuyoutEnded(Long productNo, Long finalPrice, Long buyerNo) {
        Map<String, Object> data = new HashMap<>();
        data.put("productNo", productNo);
        data.put("currentPrice", finalPrice);
        data.put("bidderNo", buyerNo);
        data.put("auctionEnded", true);
        emitterMap.forEach((clientId, emitter) ->
            sendSafe(clientId, emitter, SseEmitter.event().name("priceUpdate").data(data))
        );
    }

    /**
     * 최고 입찰자 취소 브로드캐스트.
     * 1등이 입찰을 취소하면 현재가가 2등 가격으로 낮아지며 모든 구독자에게 전파.
     * 프론트는 priceUpdate 이벤트에 bidCancelled=true 플래그가 있으면 취소 처리.
     *
     * @param productNo         상품 번호
     * @param newPrice          차순위 입찰가 (2등 가격). 2등이 없으면 시작가.
     * @param successorBidderNo 차순위 입찰자 memberNo. 없으면 null.
     */
    public void broadcastBidCancelled(Long productNo, Long newPrice, Long successorBidderNo) {
        Map<String, Object> data = new HashMap<>();
        data.put("productNo", productNo);
        data.put("currentPrice", newPrice);
        data.put("bidderNo", successorBidderNo);
        data.put("bidCancelled", true);
        emitterMap.forEach((clientId, emitter) ->
            sendSafe(clientId, emitter, SseEmitter.event().name("priceUpdate").data(data))
        );
    }

    /**
     * 판매자 경매 취소 브로드캐스트.
     * 해당 경매를 보고 있는 모든 클라이언트에게 취소 이벤트 전송.
     */
    public void broadcastAuctionCancelled(Long productNo) {
        Map<String, Object> data = Map.of("productNo", productNo);
        emitterMap.forEach((clientId, emitter) ->
            sendSafe(clientId, emitter, SseEmitter.event().name("auctionCancelled").data(data))
        );
    }
}
