package com.javajava.project.entity;

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
    private Long adminNo; // 작성 관리자 회원번호 (FK)

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "IS_DELETED", nullable = false)
    private Integer isDeleted = 0; // 1이면 삭제 처리 (목록 미노출)
}