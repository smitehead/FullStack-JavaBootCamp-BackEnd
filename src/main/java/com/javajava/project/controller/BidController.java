package com.javajava.project.controller;

import com.javajava.project.entity.BidHistory;
import com.javajava.project.service.BidService; // 인터페이스 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    public ResponseEntity<Long> placeBid(@RequestBody BidHistory bidHistory) {
        return ResponseEntity.ok(bidService.placeBid(bidHistory));
    }

    @GetMapping("/product/{productNo}")
    public ResponseEntity<List<BidHistory>> getBidHistory(@PathVariable("productNo") Long productNo) {
        return ResponseEntity.ok(bidService.getHistoryByProduct(productNo));
    }

    @PatchMapping("/{bidNo}/cancel")
    public ResponseEntity<Void> cancelBid(
            @PathVariable("bidNo") Long bidNo, 
            @RequestParam String reason) {
        bidService.cancelBid(bidNo, reason);
        return ResponseEntity.ok().build();
    }
}