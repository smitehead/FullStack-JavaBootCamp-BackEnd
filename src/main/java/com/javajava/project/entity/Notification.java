package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATION")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
    @SequenceGenerator(name = "notification_seq", sequenceName = "NOTIFICATION_SEQ", allocationSize = 1)
    @Column(name = "NOTI_NO")
    private Long notiNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo; // 수신자 회원번호 (FK)

    @Column(name = "TYPE", nullable = false, length = 30)
    private String type; // 알림 유형 (입찰/낙찰 등)

    @Column(name = "CONTENT", nullable = false, length = 500)
    private String content;

    @Column(name = "LINK_URL", length = 255)
    private String linkUrl; // 알림 클릭 시 이동할 URL

    @Column(name = "IS_READ", nullable = false)
    private Integer isRead = 0; // 1이면 읽음

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}