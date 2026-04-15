package com.javajava.project.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOTICE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_seq")
    @SequenceGenerator(name = "notice_seq", sequenceName = "NOTICE_SEQ", allocationSize = 1)
    @Column(name = "NOTICE_NO")
    private Long noticeNo;

    @Column(name = "ADMIN_NO", nullable = false)
    private Long adminNo;

    @Column(name = "CATEGORY", nullable = false, length = 20)
    private String category; // 업데이트, 이벤트, 점검, 정책

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "IS_IMPORTANT", nullable = false)
    @Builder.Default
    private Integer isImportant = 0; // 1이면 중요 공지

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "MAINTENANCE_START")
    private LocalDateTime maintenanceStart; // 점검 시작 일시 (점검 공지에만 사용)

    @Column(name = "MAINTENANCE_END")
    private LocalDateTime maintenanceEnd;   // 점검 종료 일시 (점검 공지에만 사용)

    @Column(name = "IS_DELETED", nullable = false)
    @Builder.Default
    private Integer isDeleted = 0;
}