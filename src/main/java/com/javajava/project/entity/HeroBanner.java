package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "HERO_BANNER")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "banner_seq")
    @SequenceGenerator(name = "banner_seq", sequenceName = "BANNER_SEQ", allocationSize = 1)
    @Column(name = "BANNER_NO")
    private Long bannerNo;

    @Column(name = "IMG_URL", nullable = false, length = 255)
    private String imgUrl; // 배너 이미지 URL

    @Column(name = "LINK_URL", length = 255)
    private String linkUrl; // 클릭 시 이동할 URL

    @Builder.Default
    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder = 0; // 노출 순서

    @Builder.Default
    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive = 1; // 노출 여부 (1이면 노출)

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "END_AT")
    private LocalDateTime endAt; // 노출 종료 일시 (Null이면 계속 노출)
}