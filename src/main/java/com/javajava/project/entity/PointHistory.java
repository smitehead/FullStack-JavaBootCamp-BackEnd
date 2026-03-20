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
    private Long memberNo; // 회원번호 (FK)

    @Column(name = "TYPE", nullable = false, length = 20)
    private String type; // 변동 유형 (충전/낙찰차감/판매정산 등)

    @Column(name = "AMOUNT", nullable = false)
    private Long amount; // 변동 포인트 (+/-)

    @Column(name = "BALANCE", nullable = false)
    private Long balance; // 변동 후 잔액 스냅샷

    @Column(name = "REASON", length = 100)
    private String reason; // 변동 사유

    @Builder.Default //빌더 사용 시 현재 시간 자동 할당
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    
}