package com.javajava.project.domain.auction.controller;

import com.javajava.project.domain.auction.dto.AuctionResultResponseDto;
import com.javajava.project.domain.auction.service.AuctionResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auction-results")
@RequiredArgsConstructor
public class AuctionResultController {

    private final AuctionResultService auctionResultService;

    // 상품 번호로 낙찰 결과 상세 조회 (낙찰자 본인만)
    @GetMapping("/product/{productNo}")
    public ResponseEntity<AuctionResultResponseDto> getAuctionResult(
            @PathVariable("productNo") Long productNo,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(auctionResultService.getAuctionResultByProductNo(productNo, memberNo));
    }

    // 결제 처리
    @PostMapping("/{resultNo}/pay")
    public ResponseEntity<Void> processPayment(
            @PathVariable("resultNo") Long resultNo,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        String address = body.getOrDefault("address", "");
        String addressDetail = body.getOrDefault("addressDetail", "");
        auctionResultService.processPayment(resultNo, memberNo, address, addressDetail);
        return ResponseEntity.ok().build();
    }

    // 구매 확정
    @PostMapping("/{resultNo}/confirm")
    public ResponseEntity<Void> confirmPurchase(
            @PathVariable("resultNo") Long resultNo,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        auctionResultService.confirmPurchase(resultNo, memberNo);
        return ResponseEntity.ok().build();
    }

    // 거래 취소
    @PostMapping("/{resultNo}/cancel")
    public ResponseEntity<Void> cancelTransaction(
            @PathVariable("resultNo") Long resultNo,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        auctionResultService.cancelTransaction(resultNo, memberNo);
        return ResponseEntity.ok().build();
    }
}
