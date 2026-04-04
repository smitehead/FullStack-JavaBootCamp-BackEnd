package com.javajava.project.domain.admin.service;

import org.springframework.data.domain.Page;
import com.javajava.project.domain.admin.dto.WithdrawAdminResponseDto;

public interface WithdrawAdminService {
    Page<WithdrawAdminResponseDto> getWithdrawList(String status, int page, int size);
    void processWithdraw(Long withdrawNo, String action,
                         Long adminNo, String adminNickname, String rejectReason);
}