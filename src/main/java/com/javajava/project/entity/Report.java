package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "REPORT")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "report_seq")
    @SequenceGenerator(name = "report_seq", sequenceName = "REPORT_SEQ", allocationSize = 1)
    @Column(name = "REPORT_NO")
    private Long reportNo;

    @Column(name = "REPORTER_NO", nullable = false)
    private Long reporterNo; // 신고자 회원번호 (FK)

    /**
     * 피신고자 회원번호.
     * 회원 신고 시에만 값이 있고, 상품 신고 시에는 NULL
     */
    @Column(name = "TARGET_MEMBER_NO")  // nullable = true (기본값)
    private Long targetMemberNo; // 피신고자 회원번호 (FK)

    @Column(name = "TARGET_PRODUCT_NO")
    private Long targetProductNo; // 신고 대상 상품번호 (FK - Nullable)

    @Column(name = "TYPE", nullable = false, length = 30)
    private String type; // 신고 유형 (허위매물/사기/욕설 등)

    @Column(name = "CONTENT", length = 2000)
    private String content; // 신고 내용

    @Builder.Default
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "접수"; // 처리 상태 (접수/처리중/완료/반려)

    @Column(name = "PENALTY_MSG", length = 500)
    private String penaltyMsg; // 제재 내용

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}