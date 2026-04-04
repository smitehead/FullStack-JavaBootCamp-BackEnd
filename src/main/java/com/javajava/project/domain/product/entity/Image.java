package com.javajava.project.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "IMAGE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "image_seq")
    @SequenceGenerator(name = "image_seq", sequenceName = "IMAGE_SEQ", allocationSize = 1)
    @Column(name = "IMG_NO")
    private Long imgNo;

    // 각각 어느 글에 달린 이미지인지 식별하기 위한 FK (해당되지 않으면 Null)
    @Column(name = "PRODUCT_NO")
    private Long productNo;

    @Column(name = "MEMBER_NO")
    private Long memberNo;

    @Column(name = "REPORT_NO")
    private Long reportNo;

    @Column(name = "INQUIRY_NO")
    private Long inquiryNo;

    @Column(name = "NOTICE_NO")
    private Long noticeNo;

    @Column(name = "IMG_NAME", nullable = false, length = 255)
    private String imgName; // 서버 저장 파일명 (UUID)

    @Column(name = "IMG_ORDER", nullable = false)
    private Integer imgOrder = 0; // 정렬 순서

    @Column(name = "IS_MAIN", nullable = false)
    private Integer isMain = 0; // 대표이미지 여부 (1이면 썸네일)

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}