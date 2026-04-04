package com.javajava.project.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT_IMAGE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_image_seq")
    @SequenceGenerator(name = "product_image_seq", sequenceName = "PRODUCT_IMAGE_SEQ", allocationSize = 1)
    @Column(name = "IMAGE_NO")
    private Long imageNo;

    @Column(name = "PRODUCT_NO", nullable = false)
    private Long productNo; // 연관된 상품 번호

    @Column(name = "ORIGINAL_NAME", nullable = false)
    private String originalName; // 원본 파일명 (예: photo.jpg)

    @Column(name = "UUID_NAME", nullable = false)
    private String uuidName; // 서버에 저장된 고유 파일명 (예: asdf-1234.jpg)

    @Column(name = "IMAGE_PATH", nullable = false)
    private String imagePath; // 서버 내 파일 보호 경로

    @Builder.Default
    @Column(name = "IS_MAIN", nullable = false)
    private Integer isMain = 0; // 1이면 대표 썸네일, 0이면 일반 사진

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
