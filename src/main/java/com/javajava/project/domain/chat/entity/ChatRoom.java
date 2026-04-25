package com.javajava.project.domain.chat.entity;

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

    @Builder.Default
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "ACTIVE"; // 채팅방 전체 상태 (ACTIVE/DELETED) - 양쪽 모두 나갔을 때만 DELETED

    /** 구매자가 채팅방을 나갔는지 여부 (1 = 나감, 0 = 참여 중) */
    @Builder.Default
    @Column(name = "BUYER_LEFT", nullable = false)
    private Integer buyerLeft = 0;

    /** 판매자가 채팅방을 나갔는지 여부 (1 = 나감, 0 = 참여 중) */
    @Builder.Default
    @Column(name = "SELLER_LEFT", nullable = false)
    private Integer sellerLeft = 0;

    /** 약속 상태 (1 = 약속중, 0 = 없음) */
    @Builder.Default
    @Column(name = "APPOINTMENT_STATUS", nullable = false)
    private Integer appointmentStatus = 0;

    /** 약속 일시 */
    @Column(name = "APPOINTMENT_AT")
    private LocalDateTime appointmentAt;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}