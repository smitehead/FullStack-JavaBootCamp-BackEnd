package com.javajava.project.domain.chat.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReadEvent {
    private Long roomId;
    private Long readerNo;      // 읽은 사람 회원번호
    private Long lastReadMsgNo; // 마지막으로 읽은 메시지 번호
}