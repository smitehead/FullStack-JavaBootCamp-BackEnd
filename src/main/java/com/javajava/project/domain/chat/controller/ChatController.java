package com.javajava.project.domain.chat.controller;

import com.javajava.project.domain.chat.dto.*;
import com.javajava.project.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // ══════════════════════════════════════════════════
    // REST API
    // ══════════════════════════════════════════════════

    /**
     * 채팅방 생성 (POST /api/chat/rooms)
     * 동일 조건의 ACTIVE 방이 있으면 기존 방 반환
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomListDto> createRoom(
            @RequestBody ChatRoomCreateRequest request,
            Authentication auth) {
        Long myNo = (Long) auth.getPrincipal();
        ChatRoomListDto room = chatService.createOrGetRoom(request, myNo);
        return ResponseEntity.ok(room);
    }

    /**
     * 내 채팅방 목록 조회 (GET /api/chat/rooms)
     * N+1 최적화: ROW_NUMBER() 윈도우 함수로 한 쿼리 처리
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomListDto>> getMyRooms(Authentication auth) {
        Long myNo = (Long) auth.getPrincipal();
        return ResponseEntity.ok(chatService.getMyChatRooms(myNo));
    }

    /**
     * 메시지 내역 조회 — 커서 페이징 (GET /api/chat/rooms/{roomId}/messages)
     * - lastMsgNo: 이전 페이지 마지막 메시지 번호 (없으면 최신부터)
     * - size: 조회 건수 (기본 20)
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false, defaultValue = "0") Long lastMsgNo,
            @RequestParam(required = false, defaultValue = "20") int size,
            Authentication auth) {
        Long myNo = (Long) auth.getPrincipal();
        // 참여자 검증
        if (!chatService.isParticipant(roomId, myNo)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(chatService.getMessages(roomId, lastMsgNo, size));
    }

    /**
     * 재연결 시 누락 메시지 조회 (GET /api/chat/rooms/{roomId}/messages/after)
     */
    @GetMapping("/rooms/{roomId}/messages/after")
    public ResponseEntity<List<ChatMessageDto>> getMessagesAfter(
            @PathVariable Long roomId,
            @RequestParam Long afterMsgNo,
            Authentication auth) {
        Long myNo = (Long) auth.getPrincipal();
        if (!chatService.isParticipant(roomId, myNo)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(chatService.getMessagesAfter(roomId, afterMsgNo));
    }

    /**
     * 읽음 벌크 처리 (PATCH /api/chat/rooms/{roomId}/read)
     * 방 입장 시 호출
     */
    @PatchMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            Authentication auth) {
        Long myNo = (Long) auth.getPrincipal();
        if (!chatService.isParticipant(roomId, myNo)) {
            return ResponseEntity.status(403).build();
        }
        ChatReadEvent event = chatService.markAsRead(roomId, myNo);
        // 상대방에게 실시간 읽음 알림 STOMP 발송
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId + "/read", event);
        return ResponseEntity.ok().build();
    }

    /**
     * 채팅방 Soft Delete (DELETE /api/chat/rooms/{roomId})
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId,
            Authentication auth) {
        Long myNo = (Long) auth.getPrincipal();
        chatService.deleteRoom(roomId, myNo);
        return ResponseEntity.ok().build();
    }

    // ══════════════════════════════════════════════════
    // STOMP MessageMapping
    // ══════════════════════════════════════════════════

    /**
     * 메시지 전송 (STOMP /pub/chat/message)
     *
     * 흐름: 클라이언트 발송 → DB 저장 → 브로드캐스트
     * Payload (Pub): { roomId, senderId, content, clientUuid }
     * Payload (Sub): ChatMessageDto (DB PK 포함)
     */
    @MessageMapping("/chat/message")
    public void handleMessage(ChatMessageRequest request) {
        log.info("[STOMP] 메시지 수신 시작 - roomId: {}, senderId: {}, uuid: {}, content: {}",
                request.getRoomId(), request.getSenderId(), request.getClientUuid(), request.getContent());

        try {
            // 1. DB 저장 (Save-then-Broadcast)
            ChatMessageDto savedMessage = chatService.saveMessage(request);
            log.info("[STOMP] 메시지 DB 저장 성공 - msgNo: {}", savedMessage.getMsgNo());

            // 2. 해당 채팅방 구독자에게 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/sub/chat/room/" + request.getRoomId(),
                    savedMessage
            );
            log.info("[STOMP] 메시지 브로드캐스트 완료 - roomId: {}", request.getRoomId());
            
        } catch (Exception e) {
            log.error("[STOMP] 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            // 에러 상황을 클라이언트에게 별도로 알리고 싶다면 여기서 특정 에러 토픽으로 발송 가능
        }
    }

    /**
     * 실시간 읽음 처리 (STOMP /pub/chat/read)
     * 사용자가 채팅방을 보고 있을 때 주기적으로 또는 새 메시지 수신 시 호출
     */
    @MessageMapping("/chat/read")
    public void handleRead(ChatReadEvent request) {
        ChatReadEvent event = chatService.markAsRead(request.getRoomId(), request.getReaderNo());
        // 상대방에게 읽음 상태 브로드캐스트
        messagingTemplate.convertAndSend(
                "/sub/chat/room/" + request.getRoomId() + "/read",
                event
        );
    }
}