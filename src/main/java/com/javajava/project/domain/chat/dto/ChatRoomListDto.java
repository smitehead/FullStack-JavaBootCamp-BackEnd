package com.javajava.project.domain.chat.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListDto {
    private Long roomNo;
    private Long productNo;
    private String productTitle;
    private String productImage;      // 메인 이미지 URL

    // 상대방 정보
    private Long otherUserNo;
    private String otherUserNickname;
    private String otherUserProfileImage;
    private String otherUserRole;     // "seller" 또는 "buyer"

    // 최근 메시지
    private String lastMessage;
    private LocalDateTime lastMessageAt;

    // 안 읽은 메시지 수
    private Long unreadCount;
}