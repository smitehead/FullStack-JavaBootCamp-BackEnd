package com.javajava.project.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "INQUIRY")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inquiry_seq")
    @SequenceGenerator(name = "inquiry_seq", sequenceName = "INQUIRY_SEQ", allocationSize = 1)
    @Column(name = "INQUIRY_NO")
    private Long inquiryNo; 

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo; // 문의자 회원번호 (FK)

    @Column(name = "TYPE", nullable = false, length = 20)
    private String type; // 문의 유형 (결제/계정/상품/기타)

    // 버그 유형. type이 버그 신고일 때만 사용
    @Column(name = "BUG_TYPE", length = 50)
    private String bugType;

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Lob // 내용이 길 수 있으므로 오라클의 CLOB 타입에 대응
    @Column(name = "CONTENT", nullable = false)
    private String content;

    /**
     * 답변 상태
     * 0 : 답변 대기중
     * 1 : 답변 완료
     */
    @Builder.Default
    @Column(name = "STATUS")
    private Integer status = 0;

    @Lob // 관리자 답변 역시 길 수 있으므로 CLOB 적용
    @Column(name = "ANSWER")
    private String answer; // 관리자 답변 (Null이면 미답변 상태)

    // 관리자 답변 등록 일시
    @Column(name = "ANSWERED_AT")
    private LocalDateTime answeredAt;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ADMIN_NO")
    private Long adminNo;

    @Column(name = "ADMIN_NICKNAME", length = 15)
    private String adminNickname;
}