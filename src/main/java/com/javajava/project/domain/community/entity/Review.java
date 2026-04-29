package com.javajava.project.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "REVIEW")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_seq")
    @SequenceGenerator(name = "review_seq", sequenceName = "REVIEW_SEQ", allocationSize = 1)
    @Column(name = "REVIEW_NO")
    private Long reviewNo; // 리뷰번호 (PK)

    @Column(name = "RESULT_NO", nullable = false)
    private Long resultNo; // 낙찰결과번호 (FK - AUCTION_RESULT 참조)

    @Column(name = "WRITER_NO", nullable = false)
    private Long writerNo; // 작성자 회원번호 (FK - MEMBER 참조)

    @Column(name = "TARGET_NO", nullable = false)
    private Long targetNo; // 리뷰 대상 회원번호 (FK - MEMBER 참조)

    /** 작성자 역할: BUYER(구매자→판매자 후기), SELLER(판매자→구매자 후기) */
    @Column(name = "WRITER_ROLE", nullable = false, length = 10)
    private String writerRole;

    @Column(name = "CONTENT", length = 1000)
    private String content; // 후기내용 (선택)

    @Column(name = "IS_HIDDEN", nullable = false)
    private Integer isHidden = 0; // 숨김여부 (1: 비공개)

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // 작성일시
}