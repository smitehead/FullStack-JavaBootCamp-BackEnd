package com.javajava.project.global.config;

import com.javajava.project.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * STOMP SUBSCRIBE / SEND 시 채팅방 참여자 인가 검증
 *
 * - /sub/chat/room/{roomId}: 해당 방의 buyer 또는 seller만 구독 가능
 * - /pub/chat/message: 발신자가 해당 방 참여자인지 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getUser() == null) return message;

        Long memberNo = extractMemberNo(accessor);
        if (memberNo == null) return message;

        // SUBSCRIBE 인가: /sub/chat/room/{roomId}
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/sub/chat/room/")) {
                Long roomId = extractRoomId(destination);
                if (roomId != null && !chatService.isParticipant(roomId, memberNo)) {
                    log.warn("[STOMP] 구독 거부 - memberNo: {}, roomId: {}", memberNo, roomId);
                    throw new IllegalArgumentException("해당 채팅방의 참여자가 아닙니다.");
                }
            }
        }

        return message;
    }

    private Long extractMemberNo(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    private Long extractRoomId(String destination) {
        try {
            // /sub/chat/room/123 또는 /sub/chat/room/123/read
            String[] parts = destination.split("/");
            // parts = ["", "sub", "chat", "room", "123", ...]
            if (parts.length >= 5) {
                return Long.parseLong(parts[4]);
            }
        } catch (NumberFormatException e) {
            log.warn("[STOMP] roomId 파싱 실패: {}", destination);
        }
        return null;
    }
}