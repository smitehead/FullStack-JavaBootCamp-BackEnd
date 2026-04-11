package com.javajava.project.domain.community.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "INQUIRY_IMAGE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inquiry_image_seq")
    @SequenceGenerator(name = "inquiry_image_seq", sequenceName = "INQUIRY_IMAGE_SEQ", allocationSize = 1)
    @Column(name = "IMAGE_NO")
    private Long imageNo;

    @Column(name = "INQUIRY_NO", nullable = false)
    private Long inquiryNo;

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
