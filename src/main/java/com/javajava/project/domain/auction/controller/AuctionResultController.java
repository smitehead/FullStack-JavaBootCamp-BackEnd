package com.javajava.project.domain.auction.controller;

import com.javajava.project.domain.auction.dto.AuctionResultResponseDto;
import com.javajava.project.domain.auction.dto.DeliveryAddressUpdateRequest;
import com.javajava.project.domain.auction.dto.SellerAuctionResultResponseDto;
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

    // 구매 확정 (1-step: 에스크로 정산 + 주소 저장 + 확정 통합)
    @PostMapping("/{resultNo}/confirm")
    public ResponseEntity<Void> confirmPurchase(
            @PathVariable("resultNo") Long resultNo,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        String address = body != null ? body.getOrDefault("address", "") : "";
        String addressDetail = body != null ? body.getOrDefault("addressDetail", "") : "";
        auctionResultService.confirmPurchase(resultNo, memberNo, address, addressDetail);
        return ResponseEntity.ok().build();
    }

    // 강제 승계 낙찰자 단독 취소 (isForcePromoted=1, 패널티 없음)
    @PostMapping("/{resultNo}/cancel")
    public ResponseEntity<Void> cancelTransaction(
            @PathVariable("resultNo") Long resultNo,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        auctionResultService.cancelTransaction(resultNo, memberNo);
        return ResponseEntity.ok().build();
    }

    // 일반 낙찰자 취소 요청 (isForcePromoted=0 → 상태: 취소요청)
    @PostMapping("/{resultNo}/request-cancel")
    public ResponseEntity<Void> requestCancel(
            @PathVariable("resultNo") Long resultNo,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        auctionResultService.requestCancel(resultNo, memberNo);
        return ResponseEntity.ok().build();
    }

    // 판매자 취소 승인 (구매자 취소요청 승인 — 상태: 취소요청일 때만)
    @PostMapping("/{resultNo}/cancel-approve")
    public ResponseEntity<Void> approveCancel(
            @PathVariable("resultNo") Long resultNo,
            Authentication authentication) {
        Long sellerNo = (Long) authentication.getPrincipal();
        auctionResultService.approveCancel(resultNo, sellerNo);
        return ResponseEntity.ok().build();
    }

    // 판매자 취소 요청 (배송대기 → 판매자취소요청, 구매자 동의 필요)
    @PostMapping("/{resultNo}/seller-request-cancel")
    public ResponseEntity<Void> requestCancelBySeller(
            @PathVariable("resultNo") Long resultNo,
            Authentication authentication) {
        Long sellerNo = (Long) authentication.getPrincipal();
        auctionResultService.requestCancelBySeller(resultNo, sellerNo);
        return ResponseEntity.ok().build();
    }

    // 구매자가 판매자 취소 요청 승인 (판매자취소요청 → 거래취소, 에스크로 환불)
    @PostMapping("/{resultNo}/buyer-approve-cancel")
    public ResponseEntity<Void> approveCancelByBuyer(
            @PathVariable("resultNo") Long resultNo,
            Authentication authentication) {
        Long buyerNo = (Long) authentication.getPrincipal();
        auctionResultService.approveCancelByBuyer(resultNo, buyerNo);
        return ResponseEntity.ok().build();
    }

    // 판매자 전용 낙찰 결과 조회
    @GetMapping("/seller/product/{productNo}")
    public ResponseEntity<SellerAuctionResultResponseDto> getSellerAuctionResult(
            @PathVariable("productNo") Long productNo,
            Authentication authentication) {
        Long sellerNo = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(auctionResultService.getAuctionResultBySellerAndProduct(productNo, sellerNo));
    }

    // 판매자 배송지 업데이트 (채팅에서 받은 주소 저장)
    @PatchMapping("/seller/product/{productNo}/delivery-address")
    public ResponseEntity<Void> updateDeliveryAddress(
            @PathVariable("productNo") Long productNo,
            @RequestBody DeliveryAddressUpdateRequest request,
            Authentication authentication) {
        Long sellerNo = (Long) authentication.getPrincipal();
        auctionResultService.updateDeliveryAddress(productNo, sellerNo, request.getAddrRoad(), request.getAddrDetail());
        return ResponseEntity.ok().build();
    }
}
