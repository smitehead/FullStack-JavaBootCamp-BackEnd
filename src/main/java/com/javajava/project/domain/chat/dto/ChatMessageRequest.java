package com.javajava.project.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private Long roomNo;        // 채팅방 번호
    private Long senderNo;      // 발신자 회원번호
    private String content;     // 메시지 내용 (최대 4000자)
    private String clientUuid;  // 클라이언트 생성 UUID (중복 방지)

    // 메시지 타입 (TEXT / IMAGE / LOCATION)
    private String msgType;

    // 이미지 메시지용 (msgType = IMAGE)
    private List<String> imageUrls;

    // 위치 메시지용 (msgType = LOCATION)
    private String addrRoad;
    private String addrDetail;
    private Double latitude;
    private Double longitude;
}