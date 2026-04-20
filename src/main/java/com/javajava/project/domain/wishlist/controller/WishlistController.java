package com.javajava.project.domain.wishlist.controller;

import com.javajava.project.domain.product.dto.ProductListResponseDto;
import com.javajava.project.domain.product.service.ProductService;
import com.javajava.project.domain.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final ProductService productService;

    @PostMapping("/toggle")
    public ResponseEntity<Boolean> toggleWishlist(
            @RequestParam("productNo") Long productNo,
            Authentication authentication) {
        Long memberNo = (Long) authentication.getPrincipal();
        boolean isWishlisted = wishlistService.toggleWishlist(memberNo, productNo);
        return ResponseEntity.ok(isWishlisted);
    }

    // 마이페이지: 내 찜 목록
    @GetMapping("/my")
    public ResponseEntity<Page<ProductListResponseDto>> getMyWishlist(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size) {
        Long memberNo = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(productService.getMyWishlistProducts(memberNo, page, size));
    }
}
