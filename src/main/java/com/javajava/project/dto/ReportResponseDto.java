package com.javajava.project.dto;

import com.javajava.project.entity.Report;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportResponseDto {

    private Long reportNo;
    private Long reporterNo;
    private String reporterNickname;
    private Long targetMemberNo;
    private String targetMemberNickname;
    private Long targetProductNo;
    private String type;           // 신고 유형
    private String content;        // 신고 내용
    private String status;         // 접수/처리중/완료/반려
    private String penaltyMsg;     // 제재 내용
    private LocalDateTime createdAt;

    public static ReportResponseDto from(Report r) {
        ReportResponseDto dto = new ReportResponseDto();
        dto.reportNo = r.getReportNo();
        dto.reporterNo = r.getReporterNo();
        dto.targetMemberNo = r.getTargetMemberNo();
        dto.targetProductNo = r.getTargetProductNo();
        dto.type = r.getType();
        dto.content = r.getContent();
        dto.status = r.getStatus();
        dto.penaltyMsg = r.getPenaltyMsg();
        dto.createdAt = r.getCreatedAt();
        return dto;
    }

    // 닉네임은 서비스에서 별도 세팅
    public void setReporterNickname(String nickname) {
        this.reporterNickname = nickname;
    }

    public void setTargetMemberNickname(String nickname) {
        this.targetMemberNickname = nickname;
    }
}
