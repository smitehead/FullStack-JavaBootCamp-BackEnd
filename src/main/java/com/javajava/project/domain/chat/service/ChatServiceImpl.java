package com.javajava.project.domain.chat.service;

import com.javajava.project.domain.chat.dto.*;
import com.javajava.project.domain.chat.entity.ChatMessage;
import com.javajava.project.domain.chat.entity.ChatRoom;
import com.javajava.project.domain.chat.repository.ChatMessageRepository;
import com.javajava.project.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    // ──────────────────────────────────────────────
    // 채팅방 생성 — 동시성 방어 (DB 유니크 인덱스 + 코드 방어)
    // ──────────────────────────────────────────────
    @Override
    @Transactional
    public ChatRoomListDto createOrGetRoom(ChatRoomCreateRequest request, Long myNo) {
        // 1) 이미 ACTIVE인 방이 있는지 확인
        return chatRoomRepository
                .findByBuyerNoAndSellerNoAndProductNoAndStatus(
                        request.getBuyerNo(), request.getSellerNo(),
                        request.getProductNo(), "ACTIVE")
                .map(existingRoom -> buildRoomListDto(existingRoom, myNo))
                .orElseGet(() -> {
                    // 2) 없으면 새로 생성 — DB 유니크 인덱스로 동시 INSERT 방어
                    try {
                        ChatRoom newRoom = ChatRoom.builder()
                                .buyerNo(request.getBuyerNo())
                                .sellerNo(request.getSellerNo())
                                .productNo(request.getProductNo())
                                .build();
                        chatRoomRepository.save(newRoom);
                        return buildRoomListDto(newRoom, myNo);
                    } catch (DataIntegrityViolationException e) {
                        // 동시 요청으로 이미 생성된 경우 → 기존 방 반환
                        return chatRoomRepository
                                .findByBuyerNoAndSellerNoAndProductNoAndStatus(
                                        request.getBuyerNo(), request.getSellerNo(),
                                        request.getProductNo(), "ACTIVE")
                                .map(room -> buildRoomListDto(room, myNo))
                                .orElseThrow(() -> new IllegalStateException("채팅방 생성에 실패했습니다."));
                    }
                });
    }

    // ──────────────────────────────────────────────
    // 채팅방 목록 — ROW_NUMBER() 윈도우 함수 최적화
    // ──────────────────────────────────────────────
    @Override
    public List<ChatRoomListDto> getMyChatRooms(Long myNo) {
        List<Object[]> rows = chatRoomRepository.findChatRoomListWithLatestMessage(myNo);
        return rows.stream().map(row -> ChatRoomListDto.builder()
                .roomNo(toLong(row[0]))
                .productNo(toLong(row[3]))
                .productTitle((String) row[4])
                .productImage((String) row[5])
                .lastMessage((String) row[6])
                .lastMessageAt(toLocalDateTime(row[7]))
                .unreadCount(toLong(row[9]))
                .otherUserNo(toLong(row[10]))
                .otherUserNickname((String) row[11])
                .otherUserProfileImage((String) row[12])
                .otherUserRole(
                        // row[1] = buyerNo, row[2] = sellerNo
                        // 상대방 역할: 내가 buyer면 상대는 seller, 반대면 buyer
                        toLong(row[1]).equals(myNo) ? "seller" : "buyer"
                )
                .build()
        ).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // 메시지 저장 — Save-then-Broadcast
    // ──────────────────────────────────────────────
    @Override
    @Transactional
    public ChatMessageDto saveMessage(ChatMessageRequest request) {
        // content 길이 검증 (4000자 제한)
        if (request.getContent() != null && request.getContent().length() > 4000) {
            throw new IllegalArgumentException("메시지는 4000자를 초과할 수 없습니다.");
        }

        ChatMessage message = ChatMessage.builder()
                .roomNo(request.getRoomId())
                .senderNo(request.getSenderId())
                .content(request.getContent())
                .build();

        chatMessageRepository.save(message);

        return ChatMessageDto.builder()
                .msgNo(message.getMsgNo())
                .roomNo(message.getRoomNo())
                .senderNo(message.getSenderNo())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .isRead(message.getIsRead())
                .clientUuid(request.getClientUuid()) // DB 미저장, 응답에만 포함
                .build();
    }

    // ──────────────────────────────────────────────
    // 메시지 커서 페이징 (무한 스크롤)
    // ──────────────────────────────────────────────
    @Override
    public List<ChatMessageDto> getMessages(Long roomNo, Long lastMsgNo, int size) {
        List<Object[]> rows;
        if (lastMsgNo == null || lastMsgNo == 0) {
            rows = chatMessageRepository.findLatestMessages(roomNo, size);
        } else {
            rows = chatMessageRepository.findMessagesBeforeCursor(roomNo, lastMsgNo, size);
        }

        // 결과를 오래된 순(ASC)으로 뒤집어서 반환 (프론트에서 아래→위로 그리기 위해)
        List<ChatMessageDto> result = rows.stream()
                .map(this::mapRowToMessageDto)
                .collect(Collectors.toList());
        Collections.reverse(result);
        return result;
    }

    // ──────────────────────────────────────────────
    // 재연결 시 누락 메시지 복구
    // ──────────────────────────────────────────────
    @Override
    public List<ChatMessageDto> getMessagesAfter(Long roomNo, Long afterMsgNo) {
        return chatMessageRepository.findMessagesAfter(roomNo, afterMsgNo)
                .stream()
                .map(cm -> ChatMessageDto.builder()
                        .msgNo(cm.getMsgNo())
                        .roomNo(cm.getRoomNo())
                        .senderNo(cm.getSenderNo())
                        .content(cm.getContent())
                        .sentAt(cm.getSentAt())
                        .isRead(cm.getIsRead())
                        .build())
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // 읽음 벌크 처리
    // ──────────────────────────────────────────────
    @Override
    @Transactional
    public ChatReadEvent markAsRead(Long roomNo, Long readerNo) {
        chatMessageRepository.bulkMarkAsRead(roomNo, readerNo);

        // 마지막 메시지 번호 조회 (상대방에게 알릴 용도)
        List<Object[]> latest = chatMessageRepository.findLatestMessages(roomNo, 1);
        Long lastMsgNo = latest.isEmpty() ? 0L : toLong(((Object[]) latest.get(0))[0]);

        return ChatReadEvent.builder()
                .roomId(roomNo)
                .readerNo(readerNo)
                .lastReadMsgNo(lastMsgNo)
                .build();
    }

    // ──────────────────────────────────────────────
    // Soft Delete
    // ──────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteRoom(Long roomNo, Long memberNo) {
        ChatRoom room = chatRoomRepository.findByRoomNoAndParticipant(roomNo, memberNo)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        room.setStatus("DELETED");
    }

    @Override
    public boolean isParticipant(Long roomNo, Long memberNo) {
        return chatRoomRepository.findByRoomNoAndParticipant(roomNo, memberNo).isPresent();
    }

    // ──── 유틸 메서드 ────

    private ChatRoomListDto buildRoomListDto(ChatRoom room, Long myNo) {
        Long otherNo = room.getBuyerNo().equals(myNo) ? room.getSellerNo() : room.getBuyerNo();
        return ChatRoomListDto.builder()
                .roomNo(room.getRoomNo())
                .productNo(room.getProductNo())
                .otherUserNo(otherNo)
                .otherUserRole(room.getBuyerNo().equals(myNo) ? "seller" : "buyer")
                .unreadCount(0L)
                .build();
    }

    private ChatMessageDto mapRowToMessageDto(Object[] row) {
        return ChatMessageDto.builder()
                .msgNo(toLong(row[0]))
                .roomNo(toLong(row[1]))
                .senderNo(toLong(row[2]))
                .content((String) row[3])
                .sentAt(toLocalDateTime(row[4]))
                .isRead(toInt(row[5]))
                .build();
    }

    /** Oracle NUMBER → Long 변환 (BigDecimal 대응) */
    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof BigDecimal) return ((BigDecimal) val).longValue();
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof BigDecimal) return ((BigDecimal) val).intValue();
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof Timestamp) return ((Timestamp) val).toLocalDateTime();
        return null;
    }
}