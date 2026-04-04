package com.javajava.project.domain.admin.dto;

import com.javajava.project.domain.point.entity.PointWithdraw;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class WithdrawAdminResponseDto {
    private Long withdrawNo;
    private Long memberNo;
    private String memberNickname;   // 신청자 닉네임
    private Long amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String status;
    private String adminNickname;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static WithdrawAdminResponseDto from(PointWithdraw w, String memberNickname) {
        return WithdrawAdminResponseDto.builder()
                .withdrawNo(w.getWithdrawNo())
                .memberNo(w.getMemberNo())
                .memberNickname(memberNickname)
                .amount(w.getAmount())
                .bankName(w.getBankName())
                .accountNumber(w.getAccountNumber())
                .accountHolder(w.getAccountHolder())
                .status(w.getStatus())
                .adminNickname(w.getAdminNickname())
                .rejectReason(w.getRejectReason())
                .createdAt(w.getCreatedAt())
                .processedAt(w.getProcessedAt())
                .build();
    }
}