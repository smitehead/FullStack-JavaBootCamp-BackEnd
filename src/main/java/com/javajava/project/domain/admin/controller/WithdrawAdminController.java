package com.javajava.project.domain.admin.controller;

import com.javajava.project.domain.admin.dto.WithdrawAdminResponseDto;
import com.javajava.project.domain.admin.service.WithdrawAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/withdraws")
@RequiredArgsConstructor
public class WithdrawAdminController {

    private final WithdrawAdminService withdrawAdminService;

    private Long getCurrentMemberNo() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    /**
     * 출금 신청 목록 조회 (관리자)
     * GET /api/admin/withdraws?status=전체&page=1&size=20
     */
    @GetMapping
    public ResponseEntity<Page<WithdrawAdminResponseDto>> getWithdrawList(
            @RequestParam(defaultValue = "전체") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(withdrawAdminService.getWithdrawList(status, page, size));
    }

    /**
     * 출금 처리 (관리자)
     * PATCH /api/admin/withdraws/{withdrawNo}
     * body: { action: "처리중" | "완료" | "거절", rejectReason: "..." }
     */
    @PatchMapping("/{withdrawNo}")
    public ResponseEntity<Void> processWithdraw(
            @PathVariable Long withdrawNo,
            @RequestBody Map<String, String> body) {

        Long adminNo = getCurrentMemberNo();

        // 관리자 닉네임 조회 (SecurityContext에 nickname이 없으면 memberNo로 조회 필요)
        String adminNickname = body.getOrDefault("adminNickname", "관리자");

        withdrawAdminService.processWithdraw(
                withdrawNo,
                body.get("action"),
                adminNo,
                adminNickname,
                body.get("rejectReason")
        );
        return ResponseEntity.ok().build();
    }
}