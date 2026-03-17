package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "EMD")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EupMyeonDong {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emd_seq")
    @SequenceGenerator(name = "emd_seq", sequenceName = "EMD_SEQ", allocationSize = 1)
    @Column(name = "EMD_NO")
    private Long eupMyeonDongNo;

    @Column(name = "SIGG_NO", nullable = false)
    private Long siGunGuNo; // 상위 시군구번호 (FK)

    @Column(name = "ADM_CODE", nullable = false, length = 10)
    private String admCode;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;
}