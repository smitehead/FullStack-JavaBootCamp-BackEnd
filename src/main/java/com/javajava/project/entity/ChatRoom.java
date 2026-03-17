package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_ROOM")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chatroom_seq")
    @SequenceGenerator(name = "chatroom_seq", sequenceName = "CHAT_ROOM_SEQ", allocationSize = 1)
    @Column(name = "ROOM_NO")
    private Long roomNo;

    @Column(name = "BUYER_NO", nullable = false)
    private Long buyerNo; // 구매자(입찰자) 회원번호 (FK)

    @Column(name = "SELLER_NO", nullable = false)
    private Long sellerNo; // 판매자 회원번호 (FK)

    @Column(name = "PRODUCT_NO", nullable = false)
    private Long productNo; // 채팅 대상 상품번호 (FK)

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "활성"; // 채팅방 상태 (활성/종료/차단)

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}