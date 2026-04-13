package com.javajava.project.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP CONNECT 프레임 시 JWT 토큰 검증
 *
 * 프론트에서 STOMP 연결 시 헤더에 Authorization: Bearer {token}을 포함해야 함
 * 유효한 토큰이면 memberNo를 Principal로 설정 → 이후 인터셉터에서 활용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.validateToken(token)) {
                    Long memberNo = jwtUtil.getMemberNo(token);
                    int isAdmin = jwtUtil.getIsAdmin(token);
                    List<SimpleGrantedAuthority> authorities = (isAdmin == 1)
                            ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : List.of(new SimpleGrantedAuthority("ROLE_USER"));

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(memberNo, null, authorities);
                    accessor.setUser(auth);
                    log.info("[STOMP] 인증 성공 - memberNo: {}", memberNo);
                } else {
                    log.warn("[STOMP] 유효하지 않은 토큰");
                    throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.");
                }
            } else {
                log.warn("[STOMP] Authorization 헤더 없음");
                throw new IllegalArgumentException("인증 정보가 없습니다.");
            }
        }
        return message;
    }
}