package com.javajava.project.domain.wishlist.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "WISHLIST")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wishlist_seq")
    @SequenceGenerator(name = "wishlist_seq", sequenceName = "WISHLIST_SEQ", allocationSize = 1)
    @Column(name = "WISH_NO")
    private Long wishNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo; // 찜한 회원번호 (FK)

    @Column(name = "PRODUCT_NO", nullable = false)
    private Long productNo; // 찜한 상품번호 (FK)

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}