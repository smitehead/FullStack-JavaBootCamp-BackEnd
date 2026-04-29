package com.javajava.project.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "REVIEW_TAG")
@IdClass(ReviewTagId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewTag {

    @Id
    @Column(name = "REVIEW_NO")
    private Long reviewNo;

    @Id
    @Column(name = "TAG_ID")
    private Long tagId;
}
