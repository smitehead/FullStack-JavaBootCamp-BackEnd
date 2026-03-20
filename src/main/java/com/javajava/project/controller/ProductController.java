package com.javajava.project.controller;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Long> registerProduct(@RequestBody ProductRequestDto productDto) {
        return ResponseEntity.ok(productService.save(productDto));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getProducts(
            @RequestParam(name = "sort", defaultValue = "latest") String sort) {
        return ResponseEntity.ok(productService.findAllActive(sort));
    }

    //상세 페이지 전용 데이터를 반환하도록 변경
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(
            @PathVariable("id") Long productNo,
            @RequestParam(name = "memberNo", required = false) Long currentMemberNo) {
        
        // 로그인 기능 연동 전이므로 memberNo는 파라미터로 받거나 null 처리가 가능하도록 설정
        return ResponseEntity.ok(productService.getProductDetail(productNo, currentMemberNo));
    }
    //입찰 기록 탭 구현
    @GetMapping("/{id}/bids")
    public ResponseEntity<List<ProductDetailResponseDto.BidHistoryDto>> getProductBids(
            @PathVariable("id") Long productNo) {
        return ResponseEntity.ok(productService.getBidHistory(productNo));
    }
}