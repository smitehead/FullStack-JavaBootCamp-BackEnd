package com.javajava.project.domain.bid.controller;

import com.javajava.project.domain.bid.dto.BidRequestDto;
import com.javajava.project.domain.bid.dto.BidResultDto;
import com.javajava.project.domain.bid.dto.BuyoutRequestDto;
import com.javajava.project.domain.product.dto.ProductDetailResponseDto;
import com.javajava.project.domain.bid.service.BidService;
import com.javajava.project.domain.bid.service.BidCancelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final BidCancelService bidCancelService;

    /**
     * 1. 실시간 입찰버튼
     * 성공: BidResultDto (autoBidFired, finalBidderNo, finalPrice)
     * 실패: 400 + 오류 메시지
     */
    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidRequestDto bidDto,
                                      Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        bidDto.setMemberNo(memberNo);
        try {
            BidResultDto result = bidService.processBid(bidDto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 2. 즉시구매
     * POST /api/bids/buyout
     */
    @PostMapping("/buyout")
    public ResponseEntity<String> buyout(@RequestBody BuyoutRequestDto dto,
                                         Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        String result = bidService.processBuyout(dto.getProductNo(), memberNo);
        if ("SUCCESS".equals(result)) {
            return ResponseEntity.ok("즉시 구매가 완료되었습니다.");
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 3. 특정 상품의 입찰 기록 조회
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
     * 3. 입찰 취소 (관리자/내부용)
     * - 관리자 혹은 특정 조건 하에 입찰을 취소할 때 사용합니다.
     */
    @PatchMapping("/{bidNo}/cancel")
    public ResponseEntity<Void> cancelBid(
            @PathVariable("bidNo") Long bidNo,
            @RequestParam(name = "reason") String reason) {

        bidService.cancelBid(bidNo, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * 4. 최고 입찰자 본인 입찰 취소 (Phase 1)
     * POST /api/bids/{productId}/cancel
     *
     * <p>현재 로그인한 사용자가 해당 상품의 최고 입찰자인 경우에만 취소 가능.
     * 취소 시 입찰가의 5% 위약금이 차감되어 위약금 풀에 누적됨.
     * 차순위 후보 중 포인트 충분한 첫 번째에게 즉시 낙찰 승계.
     * 마감 시간은 변경되지 않음.
     *
     * @param productId      취소할 상품 번호 (Path Variable)
     * @param authentication SecurityContext에서 추출한 인증 정보 (memberNo)
     */
    @PostMapping("/{productId}/cancel")
    public ResponseEntity<Map<String, String>> cancelMyHighestBid(
            @PathVariable("productId") Long productId,
            Authentication authentication) {

        Long memberNo = (Long) authentication.getPrincipal();
        bidCancelService.cancelHighestBid(productId, memberNo);
        return ResponseEntity.ok(Map.of("message", "입찰이 취소되었습니다. 위약금(5%)이 차감되었습니다."));
    }
}