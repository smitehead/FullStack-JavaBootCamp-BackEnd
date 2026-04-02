package com.javajava.project.controller;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.dto.ProductListResponseDto;
import com.javajava.project.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Long> registerProduct(
            @RequestPart("product") ProductRequestDto productDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {

        Long productNo = productService.save(productDto);
        if (images != null && !images.isEmpty()) {
            productService.saveImages(productNo, images);
        }
        return ResponseEntity.ok(productNo);
    }

    // 상세 페이지 전용 데이터를 반환하도록 변경
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(
            @PathVariable("id") Long productNo,
            Authentication authentication) {
        Long currentMemberNo = getMemberNoOrNull(authentication);
        return ResponseEntity.ok(productService.getProductDetail(productNo, currentMemberNo));
    }

    // 상품 목록 API (프론트엔드 필터링 및 페이징 파라미터 수신)
    @GetMapping
    public ResponseEntity<Page<ProductListResponseDto>> getProductList(
            @RequestParam(name = "page", defaultValue = "1") int page, // 리액트는 1페이지부터 시작
            @RequestParam(name = "size", defaultValue = "16") int size,
            @RequestParam(name = "large", required = false) Long large,
            @RequestParam(name = "medium", required = false) Long medium,
            @RequestParam(name = "small", required = false) Long small,
            @RequestParam(name = "minPrice", required = false) Long minPrice,
            @RequestParam(name = "maxPrice", required = false) Long maxPrice,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "delivery", required = false) Boolean delivery,
            @RequestParam(name = "face", required = false) Boolean face,
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            Authentication authentication) {
        Long memberNo = getMemberNoOrNull(authentication);
        Page<ProductListResponseDto> productPage = productService.getProductList(
                page, size, large, medium, small, minPrice, maxPrice, city, delivery, face, sort, memberNo);
        return ResponseEntity.ok(productPage);
    }

    // 입찰 기록 탭 구현
    @GetMapping("/{id}/bids")
    public ResponseEntity<List<ProductDetailResponseDto.BidHistoryDto>> getProductBids(
            @PathVariable("id") Long productNo) {
        return ResponseEntity.ok(productService.getBidHistory(productNo));
    }

    // 마이페이지: 내가 등록한 상품 목록
    @GetMapping("/my-selling")
    public ResponseEntity<List<ProductListResponseDto>> getMySellingProducts(Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(productService.getMySellingProducts(memberNo));
    }

    // 마이페이지: 내가 입찰한 상품 목록 (입찰상태 포함)
    @GetMapping("/my-bidding")
    public ResponseEntity<List<ProductListResponseDto>> getMyBiddingProducts(Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(productService.getMyBiddingProducts(memberNo));
    }

    // 마이페이지: 구매 완료(구매확정) 상품 목록
    @GetMapping("/my-purchased")
    public ResponseEntity<List<ProductListResponseDto>> getMyPurchasedProducts(Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(productService.getMyPurchasedProducts(memberNo));
    }

    // 상품 삭제 (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long productNo, Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        productService.deleteProduct(productNo, memberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * Authentication에서 memberNo 추출. 비로그인 시 null 반환.
     */
    private Long getMemberNoOrNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return (principal instanceof Long) ? (Long) principal : null;
    }
}