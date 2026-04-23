package com.javajava.project.domain.community.controller;

import com.javajava.project.domain.community.dto.ReviewRequestDto;
import com.javajava.project.domain.community.dto.ReviewResponseDto;
import com.javajava.project.domain.community.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<ReviewResponseDto> createReview(
            Authentication authentication,
            @Valid @RequestBody ReviewRequestDto dto) {
        Long memberNo = getMemberNo(authentication);
        return ResponseEntity.ok(reviewService.createReview(memberNo, dto));
    }

    /**
     * 특정 회원이 받은 리뷰 목록 (프로필/상세 페이지용)
     * GET /api/reviews/target/{memberNo}
     */
    @GetMapping("/target/{memberNo}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByTarget(
            @PathVariable("memberNo") Long memberNo) {
        return ResponseEntity.ok(reviewService.getReviewsByTarget(memberNo));
    }

    /**
     * 내가 작성한 리뷰 목록 (마이페이지용)
     * GET /api/reviews/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(Authentication authentication) {
        Long memberNo = getMemberNo(authentication);
        return ResponseEntity.ok(reviewService.getMyReviews(memberNo));
    }

    /**
     * 리뷰 숨김 처리 (삭제)
     * PUT /api/reviews/{reviewNo}/hide
     */
    @PutMapping("/{reviewNo}/hide")
    public ResponseEntity<Void> hideReview(
            Authentication authentication,
            @PathVariable("reviewNo") Long reviewNo) {
        Long memberNo = getMemberNo(authentication);
        reviewService.hideReview(memberNo, reviewNo);
        return ResponseEntity.ok().build();
    }

    private Long getMemberNo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return (Long) authentication.getPrincipal();
    }
}
