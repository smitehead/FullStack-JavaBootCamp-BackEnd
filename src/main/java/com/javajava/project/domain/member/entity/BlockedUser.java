package com.javajava.project.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BLOCKED_USER")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedUser {

    @EmbeddedId
    private BlockedUserId id;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

