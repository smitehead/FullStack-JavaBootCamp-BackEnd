package com.javajava.project.domain.chat.service;

import com.javajava.project.domain.chat.dto.*;
import java.util.List;

public interface ChatService {

    /** 채팅방 생성 (동시성 방어 포함) */
    ChatRoomListDto createOrGetRoom(ChatRoomCreateRequest request, Long myNo);

    /** 내 채팅방 목록 조회 (N+1 최적화) */
    List<ChatRoomListDto> getMyChatRooms(Long myNo);

    /** 메시지 저장 후 DTO 반환 (Save-then-Broadcast) */
    ChatMessageDto saveMessage(ChatMessageRequest request);

    /** 메시지 내역 조회 — 커서 페이징 */
    List<ChatMessageDto> getMessages(Long roomNo, Long lastMsgNo, int size);

    /** 재연결 시 누락 메시지 조회 */
    List<ChatMessageDto> getMessagesAfter(Long roomNo, Long afterMsgNo);

    /** 읽음 벌크 처리 + 마지막 읽은 msgNo 반환 */
    ChatReadEvent markAsRead(Long roomNo, Long readerNo);

    /** 채팅방 Soft Delete */
    void deleteRoom(Long roomNo, Long memberNo);

    /** 참여자 검증 (인가용) */
    boolean isParticipant(Long roomNo, Long memberNo);
}