package com.javajava.project.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "MANNER_HISTORY")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MannerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manner_history_seq")
    @SequenceGenerator(name = "manner_history_seq", sequenceName = "MANNER_HISTORY_SEQ", allocationSize = 1)
    @Column(name = "HISTORY_NO")
    private Long historyNo;

    // 매너온도가 변경된 회원번호 (FK - MEMBER 참조)
    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;

    // 변경 전 온도
    @Column(name = "PREVIOUS_TEMP", nullable = false)
    private Double previousTemp;

    // 변경 후 온도
    @Column(name = "NEW_TEMP", nullable = false)
    private Double newTemp;

    //변동 사유 
    @Column(name = "REASON", nullable = false, length = 500)
    private String reason;

    /**
     * 처리 관리자 회원번호 (FK - MEMBER 참조)
     * 거래 후기로 자동 변동 시 NULL
     */
    @Column(name = "ADMIN_NO")
    private Long adminNo;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
