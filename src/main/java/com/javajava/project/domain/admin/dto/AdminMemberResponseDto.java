package com.javajava.project.domain.admin.dto;

import com.javajava.project.domain.member.entity.Member;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminMemberResponseDto {

    private Long memberNo;
    private String userId;
    private String nickname;
    private String email;
    private String phoneNum;
    private Double mannerTemp;
    private Long points;
    private LocalDateTime joinedAt;
    private Integer isActive;
    private Integer isAdmin;
    private Integer isSuspended;
    private LocalDateTime suspendUntil;
    private String suspendReason;
    private Integer isPermanentSuspended;
    private String profileImgUrl;

    public static AdminMemberResponseDto from(Member m) {
        AdminMemberResponseDto dto = new AdminMemberResponseDto();
        dto.memberNo = m.getMemberNo();
        dto.userId = m.getUserId();
        dto.nickname = m.getNickname();
        dto.email = m.getEmail();
        dto.phoneNum = m.getPhoneNum();
        dto.mannerTemp = m.getMannerTemp();
        dto.points = m.getPoints();
        dto.joinedAt = m.getJoinedAt();
        dto.isActive = m.getIsActive();
        dto.isAdmin = m.getIsAdmin();
        dto.isSuspended = m.getIsSuspended();
        dto.suspendUntil = m.getSuspendUntil();
        dto.suspendReason = m.getSuspendReason();
        dto.isPermanentSuspended = m.getIsPermanentSuspended();
        dto.profileImgUrl = m.getProfileImgUrl();
        return dto;
    }
}
