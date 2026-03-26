package com.javajava.project.controller;

import com.javajava.project.dto.BidRequestDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    /**
     * 1. 실시간 입찰버튼
     */
    @PostMapping
    public ResponseEntity<String> placeBid(@RequestBody BidRequestDto bidDto,
                                           Authentication authentication) {
        // SecurityContext에서 인증된 사용자의 memberNo를 강제 세팅 (클라이언트 조작 방지)
        Long memberNo = (Long) authentication.getPrincipal();
        bidDto.setMemberNo(memberNo);

        String result = bidService.processBid(bidDto);
        
        if ("SUCCESS".equals(result)) {
            return ResponseEntity.ok("입찰이 성공적으로 완료되었습니다.");
        } else {
            // 실패 사유(포인트 부족, 금액 미달 등)를 반환
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 2. 특정 상품의 입찰 기록 조회
     * - 상세 페이지의 '입찰 기록' 탭을 클릭했을 때 호출됩니다.
     * - 닉네임이 포함된 최적화된 리스트를 반환
     */
    @GetMapping("/product/{productNo}")
    public ResponseEntity<List<ProductDetailResponseDto.BidHistoryDto>> getProductBidHistory(
            @PathVariable("productNo") Long productNo) {
        
        List<ProductDetailResponseDto.BidHistoryDto> history = bidService.getBidHistory(productNo);
        return ResponseEntity.ok(history);
    }

    /**
     * 3. 입찰 취소 (필요 시 사용)
     * - 관리자 혹은 특정 조건 하에 입찰을 취소할 때 사용합니다.
     */
    @PatchMapping("/{bidNo}/cancel")
    public ResponseEntity<Void> cancelBid(
            @PathVariable("bidNo") Long bidNo,
            @RequestParam(name = "reason") String reason) {
        
        bidService.cancelBid(bidNo, reason);
        return ResponseEntity.ok().build();
    }
}