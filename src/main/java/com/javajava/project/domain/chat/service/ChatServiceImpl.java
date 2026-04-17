package com.javajava.project.domain.chat.service;

import com.javajava.project.domain.chat.dto.*;
import com.javajava.project.domain.chat.entity.ChatImage;
import com.javajava.project.domain.chat.entity.ChatMessage;
import com.javajava.project.domain.chat.entity.ChatRoom;
import com.javajava.project.domain.chat.repository.ChatImageRepository;
import com.javajava.project.domain.chat.repository.ChatMessageRepository;
import com.javajava.project.domain.chat.repository.ChatRoomRepository;
import com.javajava.project.global.util.FileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatImageRepository chatImageRepository;
    private final FileStore fileStore;

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
        // content 길이 검증 (4000자 제한, IMAGE/LOCATION은 비어있어도 허용)
        if (request.getContent() != null && request.getContent().length() > 4000) {
            throw new IllegalArgumentException("메시지는 4000자를 초과할 수 없습니다.");
        }

        // 1. CHAT_MESSAGE 저장
        ChatMessage message = ChatMessage.builder()
                .roomNo(request.getRoomId())
                .senderNo(request.getSenderId())
                .content(request.getContent() != null ? request.getContent() : "")
                .msgType(request.getMsgType() != null ? request.getMsgType() : "TEXT")
                .addrRoad(request.getAddrRoad())
                .addrDetail(request.getAddrDetail())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        chatMessageRepository.save(message);

        // 2. 이미지가 있으면 CHAT_IMAGE에 bulk 저장 (순서 유지)
        List<String> savedImageUrls = new ArrayList<>();
        if ("IMAGE".equals(request.getMsgType())
                && request.getImageUrls() != null
                && !request.getImageUrls().isEmpty()) {

            List<ChatImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String url = request.getImageUrls().get(i);
                String uuidName = url.replace("/api/images/", "");
                images.add(ChatImage.builder()
                        .msgNo(message.getMsgNo())
                        .originalName(uuidName)
                        .uuidName(uuidName)
                        .imagePath(fileStore.getFullPath(uuidName))
                        .sortOrder(i) // 순서 저장
                        .build());
                savedImageUrls.add(url);
            }
            chatImageRepository.saveAll(images); // bulk insert
        }

        return ChatMessageDto.builder()
                .msgNo(message.getMsgNo())
                .roomNo(message.getRoomNo())
                .senderNo(message.getSenderNo())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .isRead(message.getIsRead())
                .msgType(message.getMsgType())
                .imageUrls(savedImageUrls)
                .addrRoad(message.getAddrRoad())
                .addrDetail(message.getAddrDetail())
                .latitude(message.getLatitude())
                .longitude(message.getLongitude())
                .clientUuid(request.getClientUuid()) // DB 미저장, 응답에만 포함
                .build();
    }

    // ──────────────────────────────────────────────
    // 메시지 커서 페이징 (무한 스크롤) — N+1 방지
    // ──────────────────────────────────────────────
    @Override
    public List<ChatMessageDto> getMessages(Long roomNo, Long lastMsgNo, int size) {
        // 1. 메시지 조회 (기존 로직)
        List<Object[]> rows = (lastMsgNo == null || lastMsgNo == 0)
                ? chatMessageRepository.findLatestMessages(roomNo, size)
                : chatMessageRepository.findMessagesBeforeCursor(roomNo, lastMsgNo, size);

        List<ChatMessageDto> messages = rows.stream()
                .map(this::mapRowToMessageDto)
                .collect(Collectors.toList());

        if (messages.isEmpty()) return messages;

        // 2. IMAGE 타입 메시지의 msgNo만 추출
        List<Long> imageMsgNos = messages.stream()
                .filter(m -> "IMAGE".equals(m.getMsgType()))
                .map(ChatMessageDto::getMsgNo)
                .collect(Collectors.toList());

        // 3. IN 쿼리로 이미지 일괄 조회 (N+1 방지)
        if (!imageMsgNos.isEmpty()) {
            List<ChatImage> allImages = chatImageRepository.findByMsgNoIn(imageMsgNos);

            // msgNo별로 그룹핑
            Map<Long, List<String>> imageMap = allImages.stream()
                    .collect(Collectors.groupingBy(
                            ChatImage::getMsgNo,
                            Collectors.mapping(
                                img -> "/api/images/" + img.getUuidName(),
                                Collectors.toList()
                            )
                    ));

            // 4. 각 메시지에 이미지 배열 매핑
            messages.forEach(m -> {
                if ("IMAGE".equals(m.getMsgType())) {
                    m.setImageUrls(imageMap.getOrDefault(m.getMsgNo(), List.of()));
                }
            });
        }

        // 결과를 오래된 순(ASC)으로 뒤집어서 반환 (프론트에서 아래→위로 그리기 위해)
        Collections.reverse(messages);
        return messages;
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
                        .msgType(cm.getMsgType())
                        .addrRoad(cm.getAddrRoad())
                        .addrDetail(cm.getAddrDetail())
                        .latitude(cm.getLatitude())
                        .longitude(cm.getLongitude())
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

    /**
     * Native Query Object[] → ChatMessageDto 변환
     * 컬럼 순서: [0]MSG_NO [1]ROOM_NO [2]SENDER_NO [3]CONTENT [4]SENT_AT [5]IS_READ
     *            [6]MSG_TYPE [7]ADDR_ROAD [8]ADDR_DETAIL [9]LATITUDE [10]LONGITUDE
     */
    private ChatMessageDto mapRowToMessageDto(Object[] row) {
        return ChatMessageDto.builder()
                .msgNo(toLong(row[0]))
                .roomNo(toLong(row[1]))
                .senderNo(toLong(row[2]))
                .content((String) row[3])
                .sentAt(toLocalDateTime(row[4]))
                .isRead(toInt(row[5]))
                .msgType(row[6] != null ? (String) row[6] : "TEXT")
                .addrRoad(row[7] != null ? (String) row[7] : null)
                .addrDetail(row[8] != null ? (String) row[8] : null)
                .latitude(toDouble(row[9]))
                .longitude(toDouble(row[10]))
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

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return ((BigDecimal) val).doubleValue();
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof Timestamp) return ((Timestamp) val).toLocalDateTime();
        return null;
    }
}