package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SIDO")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiDo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sido_seq")
    @SequenceGenerator(name = "sido_seq", sequenceName = "SIDO_SEQ", allocationSize = 1)
    @Column(name = "SIDO_NO")
    private Long siDoNo;

    @Column(name = "ADM_CODE", nullable = false, length = 2)
    private String admCode;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;
}