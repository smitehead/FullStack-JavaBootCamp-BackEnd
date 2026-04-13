package com.javajava.project.domain.chat.dto;

import lombok.*;
import java.time.LocalDateTime;

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
}