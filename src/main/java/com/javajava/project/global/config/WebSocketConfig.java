package com.javajava.project.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;
    private final StompChannelInterceptor stompChannelInterceptor;

    /**
     * STOMP 엔드포인트 등록
     * - /ws-stomp: WebSocket 연결 경로
     * - SockJS fallback: WebSocket 미지원 브라우저 대응
     * - 허용 Origin: 기존 SecurityConfig CORS와 동일
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://54.164.62.214:*"
                )
                .withSockJS();
    }

    /**
     * 메시지 브로커 설정
     * - /sub: 클라이언트가 구독하는 prefix (subscribe)
     * - /pub: 클라이언트가 메시지를 보내는 prefix (publish)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");       // 구독 경로 prefix
        registry.setApplicationDestinationPrefixes("/pub"); // 발행 경로 prefix
    }

    /**
     * STOMP CONNECT 시 JWT 인증 인터셉터 등록
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor, stompChannelInterceptor);
    }
}