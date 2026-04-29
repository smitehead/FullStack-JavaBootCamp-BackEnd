package com.javajava.project.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "REVIEW_TAG_DEF")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewTagDef {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_tag_def_seq")
    @SequenceGenerator(name = "review_tag_def_seq", sequenceName = "REVIEW_TAG_DEF_SEQ", allocationSize = 1)
    @Column(name = "TAG_ID")
    private Long tagId;

    @Column(name = "TAG_NAME", nullable = false, length = 50)
    private String tagName;

    /** 태그 적용 대상 역할: BUYER(구매자→판매자), SELLER(판매자→구매자), ALL(공통) */
    @Column(name = "APPLICABLE_ROLE", nullable = false, length = 10)
    private String applicableRole;
}
