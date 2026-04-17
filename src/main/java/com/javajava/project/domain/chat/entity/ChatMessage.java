package com.javajava.project.domain.chat.entity;

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

    @Builder.Default
    @Column(name = "IS_READ", nullable = false)
    private Integer isRead = 0; // 읽음 여부 (1: 읽음)

    @Column(name = "SENT_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now(); // 전송일시

    @Column(name = "CONTENT", nullable = false, length = 4000)
    private String content;

    @Builder.Default
    @Column(name = "MSG_TYPE", nullable = false, length = 20)
    private String msgType = "TEXT"; // TEXT / IMAGE / LOCATION

    @Column(name = "ADDR_ROAD", length = 200)
    private String addrRoad;

    @Column(name = "ADDR_DETAIL", length = 255)
    private String addrDetail;

    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;
}