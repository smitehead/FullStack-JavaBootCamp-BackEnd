package com.javajava.project.controller;

import com.javajava.project.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/toggle")
    public ResponseEntity<Boolean> toggleWishlist(
            @RequestParam("memberNo") Long memberNo,
            @RequestParam("productNo") Long productNo) {
        
        boolean isWishlisted = wishlistService.toggleWishlist(memberNo, productNo);
        return ResponseEntity.ok(isWishlisted);
    }
}
