package com.javajava.project.domain.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "REPORT_IMAGE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "report_image_seq")
    @SequenceGenerator(name = "report_image_seq", sequenceName = "REPORT_IMAGE_SEQ", allocationSize = 1)
    @Column(name = "IMAGE_NO")
    private Long imageNo;

    @Column(name = "REPORT_NO", nullable = false)
    private Long reportNo;

    @Column(name = "ORIGINAL_NAME", nullable = false, length = 255)
    private String originalName;

    @Column(name = "UUID_NAME", nullable = false, length = 255)
    private String uuidName;

    @Column(name = "IMAGE_PATH", nullable = false, length = 500)
    private String imagePath;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
