package com.javajava.project.domain.chat.controller;

import com.javajava.project.domain.chat.dto.*;
import com.javajava.project.domain.chat.service.ChatService;
import com.javajava.project.global.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final FileStore fileStore;

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
     * 채팅 이미지 업로드 (POST /api/chat/rooms/{roomId}/images)
     * 여러 파일을 한 번에 업로드하여 URL 배열 반환
     * STOMP 전송 전에 먼저 호출해야 함
     */
    @PostMapping("/rooms/{roomId}/images")
    public ResponseEntity<?> uploadChatImages(
            @PathVariable Long roomId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication auth) {
        Long myNo = (Long) auth.getPrincipal();
        if (!chatService.isParticipant(roomId, myNo)) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<String> urls = new ArrayList<>();
            for (MultipartFile file : files) {
                String uuidName = fileStore.storeGenericFile(file);
                urls.add("/api/images/" + uuidName);
            }
            return ResponseEntity.ok(Map.of("urls", urls));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "파일 저장 실패"));
        }
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
    public void handleMessage(ChatMessageRequest request, Authentication auth) {
        // 인증 정보 없으면 즉시 거부 (NullPointerException 방지)
        if (auth == null || auth.getPrincipal() == null) {
            log.warn("[STOMP] 인증 정보 없음 - 메시지 처리 거부 (uuid: {})", request.getClientUuid());
            if (request.getClientUuid() != null && request.getRoomId() != null) {
                messagingTemplate.convertAndSend("/sub/chat/room/" + request.getRoomId(),
                        ChatMessageDto.builder().clientUuid(request.getClientUuid())
                                .msgType("ERROR").content("인증 정보가 없습니다.").build());
            }
            return;
        }

        // [Security] 클라이언트가 보낸 senderId 대신 인증 세션(Principal)의 memberNo를 사용
        Long senderId = (Long) auth.getPrincipal();

        log.info("[STOMP] 메시지 수신 시작 - roomId: {}, senderId(Auth): {}, uuid: {}, content: {}",
                request.getRoomId(), senderId, request.getClientUuid(), request.getContent());

        try {
            // 1. DB 저장 (Save-then-Broadcast) - 서비스 레벨에서 참여자 검증 수행
            ChatMessageDto savedMessage = chatService.saveMessage(request, senderId);
            log.info("[STOMP] 메시지 DB 저장 성공 - msgNo: {}", savedMessage.getMsgNo());

            // 2. 해당 채팅방 구독자에게 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/sub/chat/room/" + request.getRoomId(),
                    savedMessage
            );
            log.info("[STOMP] 메시지 브로드캐스트 완료 - roomId: {}", request.getRoomId());

        } catch (Exception e) {
            log.error("[STOMP] 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시 발신자에게 FAILED 응답 전송 (clientUuid로 낙관적 메시지 매칭)
            if (request.getClientUuid() != null) {
                ChatMessageDto errorMsg = ChatMessageDto.builder()
                        .clientUuid(request.getClientUuid())
                        .msgType("ERROR")
                        .content(e.getMessage())
                        .build();
                messagingTemplate.convertAndSend("/sub/chat/room/" + request.getRoomId(), errorMsg);
            }
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