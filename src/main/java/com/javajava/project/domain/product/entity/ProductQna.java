package com.javajava.project.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QNA")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductQna {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qna_seq")
    @SequenceGenerator(name = "qna_seq", sequenceName = "QNA_SEQ", allocationSize = 1)
    @Column(name = "PRDT_QNA_NO")
    private Long prdtQnaNo;

    @Column(name = "BID_NO")
    private Long bidNo;

    @Column(name = "PRODUCT_NO", nullable = false)
    private Long productNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;

    @Column(name = "CONTENT", length = 500)
    private String content;

    @Column(name = "ANSWER", length = 1000)
    private String answer;

    @Column(name = "ANSWERED_AT")
    private LocalDateTime answeredAt;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
