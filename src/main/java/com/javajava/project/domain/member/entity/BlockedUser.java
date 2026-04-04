package com.javajava.project.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BLOCKED_USER")
@IdClass(BlockedUserId.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedUser {

    // 차단한 회원번호 (복합 PK, FK - MEMBER 참조)
    @Id
    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;

    // 차단된 회원번호 (복합 PK, FK - MEMBER 참조)
    @Id
    @Column(name = "BLOCKED_MEMBER_NO", nullable = false)
    private Long blockedMemberNo;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
