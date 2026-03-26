package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "QNA")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Qna {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qna_seq")
    @SequenceGenerator(name = "qna_seq", sequenceName = "QNA_SEQ", allocationSize = 1)
    @Column(name = "PRDT_QNA_NO")
    private Long prdtQnaNo;

    /**
     * 입찰번호 (FK - BID_HISTORY 참조)
     * 입찰자가 남긴 문의일 경우만 값이 있고, 일반 문의는 NULL
     */
    @Column(name = "BID_NO")
    private Long bidNo;

    // 문의 대상 상품번호 (FK - PRODUCT 참조)
    @Column(name = "PRODUCT_NO", nullable = false)
    private Long productNo;

    // 문의 내용
    @Column(name = "CONTENT", length = 500)
    private String content;
}
