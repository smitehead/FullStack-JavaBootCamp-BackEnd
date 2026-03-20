package com.javajava.project.controller;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.dto.ProductListResponseDto;
import com.javajava.project.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    //상세 페이지 전용 데이터를 반환하도록 변경
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(
            @PathVariable("id") Long productNo,
            @RequestParam(name = "memberNo", required = false) Long currentMemberNo) {
        // 로그인 기능 연동 전이므로 memberNo는 파라미터로 받거나 null 처리가 가능하도록 설정
        return ResponseEntity.ok(productService.getProductDetail(productNo, currentMemberNo));
    }

    // 상품 목록 API (프론트엔드 필터링 및 페이징 파라미터 수신)
    @GetMapping
    public ResponseEntity<Page<ProductListResponseDto>> getProductList(
            @RequestParam(defaultValue = "1") int page,      // 리액트는 1페이지부터 시작
            @RequestParam(defaultValue = "16") int size,
            @RequestParam(required = false) Long large,
            @RequestParam(required = false) Long medium,
            @RequestParam(required = false) Long small,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean delivery,
            @RequestParam(required = false) Boolean face,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) Long memberNo // 찜 여부 확인용
    ) {
        Page<ProductListResponseDto> productPage = productService.getProductList(
                page, size, large, medium, small, minPrice, maxPrice, city, delivery, face, sort, memberNo);
        return ResponseEntity.ok(productPage);
    }

    //입찰 기록 탭 구현
    @GetMapping("/{id}/bids")
    public ResponseEntity<List<ProductDetailResponseDto.BidHistoryDto>> getProductBids(
            @PathVariable("id") Long productNo) {
        return ResponseEntity.ok(productService.getBidHistory(productNo));
    }
}