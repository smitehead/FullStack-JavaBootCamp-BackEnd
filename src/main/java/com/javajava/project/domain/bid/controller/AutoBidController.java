package com.javajava.project.domain.bid.controller;

import com.javajava.project.domain.bid.dto.AutoBidRequestDto;
import com.javajava.project.domain.bid.dto.AutoBidResponseDto;
import com.javajava.project.domain.bid.service.AutoBidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auto-bid")
@RequiredArgsConstructor
public class AutoBidController {

    private final AutoBidService autoBidService;

    /**
     * 자동입찰 등록 / 수정
     * 동일·하위 금액, 포인트 부족 등 오류는 400으로 반환
     */
    @PostMapping
    public ResponseEntity<?> registerAutoBid(
            @RequestBody AutoBidRequestDto dto,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        try {
            AutoBidResponseDto result = autoBidService.registerAutoBid(memberNo, dto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 자동입찰 취소
     */
    @DeleteMapping("/{productNo}")
    public ResponseEntity<Void> cancelAutoBid(
            @PathVariable("productNo") Long productNo,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        autoBidService.cancelAutoBid(memberNo, productNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 활성 자동입찰 조회 (상품 상세 페이지 진입 시)
     */
    @GetMapping("/active")
    public ResponseEntity<AutoBidResponseDto> getActiveAutoBid(
            @RequestParam("productNo") Long productNo,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        Optional<AutoBidResponseDto> result = autoBidService.getActiveAutoBid(memberNo, productNo);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
