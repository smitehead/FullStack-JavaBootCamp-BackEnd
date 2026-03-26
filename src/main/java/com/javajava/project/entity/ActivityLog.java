package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACTIVITY_LOG")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "activity_log_seq")
    @SequenceGenerator(name = "activity_log_seq", sequenceName = "ACTIVITY_LOG_SEQ", allocationSize = 1)
    @Column(name = "LOG_NO")
    private Long logNo;

    // 행동을 수행한 관리자 회원번호 (FK - MEMBER 참조)
    @Column(name = "ADMIN_NO", nullable = false)
    private Long adminNo;

    // 수행 행동. 예: "사용자 정지", "경매 강제 종료", "신고 처리"
    @Column(name = "ACTION", nullable = false, length = 100)
    private String action;

    // 행동 대상 식별자 (대상이 없는 경우 NULL)
    @Column(name = "TARGET_ID")
    private Long targetId;

    /**
     * 행동 대상 유형
     * 예: "user", "product", "notice", "inquiry", "report"
     */
    @Column(name = "TARGET_TYPE", length = 20)
    private String targetType;

    // 상세 내역 (긴 텍스트 가능)
    @Lob
    @Column(name = "DETAILS")
    private String details;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
