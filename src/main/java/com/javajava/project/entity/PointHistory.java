package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "POINT_HISTORY")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "point_seq")
    @SequenceGenerator(name = "point_seq", sequenceName = "POINT_SEQ", allocationSize = 1)
    @Column(name = "POINT_NO")
    private Long pointNo;

    @Column(name = "MEMBER_NO", nullable = false)
    private Long memberNo;

    @Column(name = "TYPE", nullable = false, length = 20)
    private String type;

    @Column(name = "AMOUNT", nullable = false)
    private Long amount;

    @Column(name = "BALANCE", nullable = false)
    private Long balance;

    @Column(name = "REASON", length = 100)
    private String reason;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}