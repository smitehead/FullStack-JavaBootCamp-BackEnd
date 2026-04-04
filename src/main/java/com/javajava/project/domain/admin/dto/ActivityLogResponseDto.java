package com.javajava.project.domain.admin.dto;

import com.javajava.project.domain.admin.entity.ActivityLog;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ActivityLogResponseDto {

    private Long logNo;
    private Long adminNo;
    private String adminNickname;
    private String action;
    private Long targetId;
    private String targetType;
    private String details;
    private LocalDateTime createdAt;

    public static ActivityLogResponseDto from(ActivityLog log) {
        ActivityLogResponseDto dto = new ActivityLogResponseDto();
        dto.logNo = log.getLogNo();
        dto.adminNo = log.getAdminNo();
        dto.action = log.getAction();
        dto.targetId = log.getTargetId();
        dto.targetType = log.getTargetType();
        dto.details = log.getDetails();
        dto.createdAt = log.getCreatedAt();
        return dto;
    }

    public void setAdminNickname(String nickname) {
        this.adminNickname = nickname;
    }
}
