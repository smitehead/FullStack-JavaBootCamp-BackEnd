package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SIGG")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiGunGu {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sigg_seq")
    @SequenceGenerator(name = "sigg_seq", sequenceName = "SIGG_SEQ", allocationSize = 1)
    @Column(name = "SIGG_NO")
    private Long siGunGuNo;

    @Column(name = "SIDO_NO", nullable = false)
    private Long siDoNo; // 상위 시도번호 (FK)

    @Column(name = "ADM_CODE", nullable = false, length = 5)
    private String admCode;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;
}