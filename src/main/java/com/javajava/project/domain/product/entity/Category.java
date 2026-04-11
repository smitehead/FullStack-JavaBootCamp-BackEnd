package com.javajava.project.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CATEGORY")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @Column(name = "CATEGORY_NO")
    private Long categoryNo;

    @Column(name = "PARENT_NO")
    private Long parentNo; // 상위 카테고리번호 (자기참조, 대분류는 Null)

    @Column(name = "NAME", nullable = false, length = 30)
    private String name;

    @Column(name = "DEPTH", nullable = false)
    private Integer depth; // 계층 깊이 (1:대분류, 2:중분류, 3:소분류)
}