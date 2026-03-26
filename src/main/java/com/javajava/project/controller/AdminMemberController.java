package com.javajava.project.controller;

import com.javajava.project.dto.*;
import com.javajava.project.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 전용 회원 관리 API
 * - 회원 목록 조회/검색
 * - 정지/해제
 * - 매너온도 변경
 * - 포인트 변경
 * - 권한 변경
 * - 매너온도 변동 이력 조회
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminService adminService;

    /**
     * 전체 회원 목록 조회 (검색 키워드 있으면 검색)
     * GET /api/admin/members?keyword=xxx
     */
    @GetMapping
    public ResponseEntity<List<AdminMemberResponseDto>> getMembers(
            @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(adminService.searchMembers(keyword));
        }
        return ResponseEntity.ok(adminService.getAllMembers());
    }

    /**
     * 회원 정지
     * PUT /api/admin/members/{memberNo}/suspend
     */
    @PutMapping("/{memberNo}/suspend")
    public ResponseEntity<Void> suspendMember(
            @PathVariable Long memberNo,
            @Valid @RequestBody SuspendRequestDto dto,
            Authentication authentication) {
        Long adminNo = getAdminNo(authentication);
        adminService.suspendMember(memberNo, dto.getSuspendDays(), dto.getSuspendReason(), adminNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 정지 해제
     * PUT /api/admin/members/{memberNo}/unsuspend
     */
    @PutMapping("/{memberNo}/unsuspend")
    public ResponseEntity<Void> unsuspendMember(
            @PathVariable Long memberNo,
            Authentication authentication) {
        Long adminNo = getAdminNo(authentication);
        adminService.unsuspendMember(memberNo, adminNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 매너온도 변경
     * PUT /api/admin/members/{memberNo}/manner-temp
     */
    @PutMapping("/{memberNo}/manner-temp")
    public ResponseEntity<Void> updateMannerTemp(
            @PathVariable Long memberNo,
            @Valid @RequestBody MannerTempRequestDto dto,
            Authentication authentication) {
        Long adminNo = getAdminNo(authentication);
        adminService.updateMannerTemp(memberNo, dto.getNewTemp(), dto.getReason(), adminNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 포인트 변경
     * PUT /api/admin/members/{memberNo}/points
     */
    @PutMapping("/{memberNo}/points")
    public ResponseEntity<Void> updatePoints(
            @PathVariable Long memberNo,
            @Valid @RequestBody PointsRequestDto dto,
            Authentication authentication) {
        Long adminNo = getAdminNo(authentication);
        adminService.updatePoints(memberNo, dto.getPointAmount(), adminNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 권한 변경
     * PUT /api/admin/members/{memberNo}/role
     */
    @PutMapping("/{memberNo}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable Long memberNo,
            @RequestBody java.util.Map<String, Integer> body,
            Authentication authentication) {
        Long adminNo = getAdminNo(authentication);
        Integer isAdmin = body.get("isAdmin");
        if (isAdmin == null) {
            throw new IllegalArgumentException("isAdmin 값은 필수입니다.");
        }
        adminService.updateRole(memberNo, isAdmin, adminNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 매너온도 변동 이력 조회 (전체 또는 특정 회원)
     * GET /api/admin/members/manner-history?memberNo=xxx
     */
    @GetMapping("/manner-history")
    public ResponseEntity<List<MannerHistoryResponseDto>> getMannerHistory(
            @RequestParam(required = false) Long memberNo) {
        if (memberNo != null) {
            return ResponseEntity.ok(adminService.getMannerHistoryByMember(memberNo));
        }
        return ResponseEntity.ok(adminService.getAllMannerHistory());
    }

    private Long getAdminNo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return (Long) authentication.getPrincipal();
    }
}
