package com.javajava.project.domain.point.controller;

import com.javajava.project.domain.admin.dto.WithdrawAdminResponseDto;
import com.javajava.project.domain.point.dto.BankAccountDto;
import com.javajava.project.domain.point.dto.BillingKeyRegisterRequestDto;
import com.javajava.project.domain.point.dto.BillingKeyResponseDto;
import com.javajava.project.domain.point.dto.ChargeRequestDto;
import com.javajava.project.domain.point.dto.ChargeResponseDto;
import com.javajava.project.domain.point.dto.PointHistoryResponseDto;
import com.javajava.project.domain.point.dto.WithdrawRequestDto;
import com.javajava.project.domain.point.dto.WithdrawResponseDto;
import com.javajava.project.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

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

    /** 계좌 목록 조회 */
    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccountDto>> getAccounts() {
        return ResponseEntity.ok(pointService.getAccounts(getCurrentMemberNo()));
    }

    /** 계좌 추가 */
    @PostMapping("/accounts")
    public ResponseEntity<Void> addAccount(@RequestBody BankAccountDto dto) {
        pointService.addAccount(getCurrentMemberNo(), dto);
        return ResponseEntity.ok().build();
    }

    /** 계좌 삭제 */
    @DeleteMapping("/accounts/{accountNo}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long accountNo) {
        pointService.deleteAccount(getCurrentMemberNo(), accountNo);
        return ResponseEntity.ok().build();
    }

    /** 출금 신청 */
    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawResponseDto> withdraw(@RequestBody WithdrawRequestDto dto) {
        return ResponseEntity.ok(pointService.withdraw(getCurrentMemberNo(), dto));
    }
}