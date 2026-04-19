package com.javajava.project.domain.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PLATFORM_REVENUE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformRevenue {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "platform_revenue_seq")
    @SequenceGenerator(name = "platform_revenue_seq", sequenceName = "PLATFORM_REVENUE_SEQ", allocationSize = 1)
    @Column(name = "REVENUE_NO")
    private Long revenueNo;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Column(name = "REASON", nullable = false, length = 255)
    private String reason;

    @Column(name = "SOURCE_MEMBER_NO")
    private Long sourceMemberNo;

    @Column(name = "RELATED_PRODUCT_NO")
    private Long relatedProductNo;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
