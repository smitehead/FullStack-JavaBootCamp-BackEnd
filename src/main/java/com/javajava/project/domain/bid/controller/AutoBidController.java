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
     */
    @PostMapping
    public ResponseEntity<AutoBidResponseDto> registerAutoBid(
            @RequestBody AutoBidRequestDto dto,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        AutoBidResponseDto result = autoBidService.registerAutoBid(memberNo, dto);
        return ResponseEntity.ok(result);
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
