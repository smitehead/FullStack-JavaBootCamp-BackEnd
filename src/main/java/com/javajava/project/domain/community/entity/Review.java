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

    @Column(name = "RATING")
    private Integer rating; // 별점 (1~5점, 선택)

    @Column(name = "TAGS", length = 500)
    private String tags; // 태그 (콤마 구분, 예: "응답이 빨라요,친절하고 매너가 좋아요")

    @Column(name = "CONTENT", length = 1000)
    private String content; // 후기내용 (선택)

    @Column(name = "IS_HIDDEN", nullable = false)
    private Integer isHidden = 0; // 숨김여부 (1: 비공개)

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // 작성일시
}