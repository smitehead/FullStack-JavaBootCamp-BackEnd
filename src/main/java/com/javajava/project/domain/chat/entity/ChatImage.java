package com.javajava.project.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_IMAGE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_image_seq")
    @SequenceGenerator(name = "chat_image_seq", sequenceName = "CHAT_IMAGE_SEQ", allocationSize = 1)
    @Column(name = "IMAGE_NO")
    private Long imageNo;

    @Column(name = "MSG_NO", nullable = false)
    private Long msgNo;

    @Column(name = "ORIGINAL_NAME", nullable = false)
    private String originalName;

    @Column(name = "UUID_NAME", nullable = false)
    private String uuidName;

    @Column(name = "IMAGE_PATH", nullable = false)
    private String imagePath;

    @Builder.Default
    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
