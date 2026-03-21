package com.javajava.project.dto;

import com.javajava.project.entity.Notification;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class NotificationResponseDto {

    private Long notiNo;
    private String type;
    private String content;
    private String linkUrl;
    private Integer isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification n) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.notiNo = n.getNotiNo();
        dto.type = n.getType();
        dto.content = n.getContent();
        dto.linkUrl = n.getLinkUrl();
        dto.isRead = n.getIsRead();
        dto.createdAt = n.getCreatedAt();
        return dto;
    }
}
