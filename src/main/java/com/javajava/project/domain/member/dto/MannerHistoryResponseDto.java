package com.javajava.project.domain.member.dto;

import com.javajava.project.domain.member.entity.MannerHistory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MannerHistoryResponseDto {

    private Long historyNo;
    private Long memberNo;
    private String memberNickname;
    private Double previousTemp;
    private Double newTemp;
    private String reason;
    private Long adminNo;
    private String adminNickname;
    private LocalDateTime createdAt;

    public static MannerHistoryResponseDto from(MannerHistory h) {
        MannerHistoryResponseDto dto = new MannerHistoryResponseDto();
        dto.historyNo = h.getHistoryNo();
        dto.memberNo = h.getMemberNo();
        dto.previousTemp = h.getPreviousTemp();
        dto.newTemp = h.getNewTemp();
        dto.reason = h.getReason();
        dto.adminNo = h.getAdminNo();
        dto.createdAt = h.getCreatedAt();
        return dto;
    }

    // 닉네임은 서비스에서 별도 세팅
    public void setMemberNickname(String nickname) {
        this.memberNickname = nickname;
    }

    public void setAdminNickname(String nickname) {
        this.adminNickname = nickname;
    }
}
