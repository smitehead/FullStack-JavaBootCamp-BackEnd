package com.javajava.project.domain.point.controller;

import com.javajava.project.domain.point.dto.BillingKeyRegisterRequestDto;
import com.javajava.project.domain.point.dto.BillingKeyResponseDto;
import com.javajava.project.domain.point.dto.ChargeRequestDto;
import com.javajava.project.domain.point.dto.ChargeResponseDto;
import com.javajava.project.domain.point.dto.PointHistoryResponseDto;
import com.javajava.project.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    // SecurityContext에서 memberNo 추출 (JwtAuthenticationFilter가 등록해 둔 값)
    private Long getCurrentMemberNo() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    // 카드 등록 (빌링키 저장)
    @PostMapping("/billing-key")
    public ResponseEntity<Void> registerBillingKey(@RequestBody BillingKeyRegisterRequestDto dto) {
        pointService.registerBillingKey(getCurrentMemberNo(), dto);
        return ResponseEntity.ok().build();
    }

    // 등록된 카드 조회 (Settings.tsx 카드 탭 표시용)
    @GetMapping("/billing-key")
    public ResponseEntity<BillingKeyResponseDto> getBillingKey() {
        return ResponseEntity.ok(pointService.getBillingKey(getCurrentMemberNo()));
    }

    // 카드 삭제
    @DeleteMapping("/billing-key")
    public ResponseEntity<Void> deleteBillingKey() {
        pointService.deleteBillingKey(getCurrentMemberNo());
        return ResponseEntity.ok().build();
    }

    // 포인트 충전
    @PostMapping("/charge")
    public ResponseEntity<ChargeResponseDto> charge(@RequestBody ChargeRequestDto dto) {
        return ResponseEntity.ok(pointService.charge(getCurrentMemberNo(), dto));
    }

    // 포인트 내역 조회
    @GetMapping("/history")
    public ResponseEntity<Page<PointHistoryResponseDto>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(pointService.getHistory(getCurrentMemberNo(), page, size));
    }
}