package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_MESSAGE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chatmsg_seq")
    @SequenceGenerator(name = "chatmsg_seq", sequenceName = "CHAT_MESSAGE_SEQ", allocationSize = 1)
    @Column(name = "MSG_NO")
    private Long msgNo;

    @Column(name = "ROOM_NO", nullable = false)
    private Long roomNo; // 채팅방번호 (FK)

    @Column(name = "SENDER_NO", nullable = false)
    private Long senderNo; // 발신자 회원번호 (FK)

    @Column(name = "IS_READ", nullable = false)
    private Integer isRead = 0; // 읽음 여부 (1: 읽음)

    @Column(name = "SENT_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now(); // 전송일시

    @Column(name = "CONTENT", nullable = false, length = 1000)
    private String content; // 메시지 내용
}