package com.javajava.project.domain.chat.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long msgNo;           // DB PK (커서 페이징 기준)
    private Long roomNo;
    private Long senderNo;
    private String content;
    private LocalDateTime sentAt;
    private Integer isRead;
    private String clientUuid;    // 프론트 중복 방지용 UUID (DB 미저장)

    // 메시지 타입 (TEXT / IMAGE / LOCATION)
    private String msgType;

    // 이미지 메시지 (msgType = IMAGE)
    private List<String> imageUrls;

    // 위치 메시지 (msgType = LOCATION)
    private String addrRoad;
    private String addrDetail;
    private Double latitude;
    private Double longitude;

    // 약속 일시 (msgType = APPOINTMENT)
    private LocalDateTime apptAt;
}