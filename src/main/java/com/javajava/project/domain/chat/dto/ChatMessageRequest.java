package com.javajava.project.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private Long roomId;        // 채팅방 번호
    private Long senderId;      // 발신자 회원번호
    private String content;     // 메시지 내용 (최대 4000자)
    private String clientUuid;  // 클라이언트 생성 UUID (중복 방지)
}