package com.javajava.project.domain.community.dto;

import com.javajava.project.domain.community.entity.Notice;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeResponseDto {

    private Long id;
    private String category;
    private String title;
    private String description;
    private String content;
    private Boolean isImportant;
    private LocalDateTime createdAt;
    private Integer isDeleted;

    public static NoticeResponseDto from(Notice notice) {
        return NoticeResponseDto.builder()
                .id(notice.getNoticeNo())
                .category(notice.getCategory())
                .title(notice.getTitle())
                .description(notice.getDescription())
                .content(notice.getContent())
                .isImportant(notice.getIsImportant() != null && notice.getIsImportant() == 1)
                .createdAt(notice.getCreatedAt())
                .isDeleted(notice.getIsDeleted())
                .build();
    }
}
