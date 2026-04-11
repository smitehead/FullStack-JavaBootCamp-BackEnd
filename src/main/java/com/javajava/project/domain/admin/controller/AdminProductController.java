package com.javajava.project.domain.admin.controller;

import com.javajava.project.domain.admin.dto.AdminProductResponseDto;
import com.javajava.project.domain.admin.entity.ActivityLog;
import com.javajava.project.domain.admin.repository.ActivityLogRepository;
import com.javajava.project.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final ActivityLogRepository activityLogRepository;

    /**
     * 관리자용 전체 상품 목록 조회 (삭제 제외, 최신순)
     * GET /api/admin/products
     */
    @GetMapping
    public ResponseEntity<List<AdminProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProductsForAdmin());
    }

    /**
     * 관리자 대시보드: 대분류별 상품 건수
     * GET /api/admin/products/category-stats
     */
    @GetMapping("/category-stats")
    public ResponseEntity<List<Map<String, Object>>> getCategoryStats() {
        return ResponseEntity.ok(productService.getCategoryStats());
    }

    /**
     * 경매 강제 종료
     * PUT /api/admin/products/{productNo}/cancel
     */
    @PutMapping("/{productNo}/cancel")
    public ResponseEntity<Void> cancelAuction(@PathVariable("productNo") Long productNo,
                                               Authentication authentication) {
        productService.cancelAuctionByAdmin(productNo);
        logActivity(authentication, "경매 강제 종료", productNo, "경매 강제 종료: #" + productNo);
        return ResponseEntity.ok().build();
    }

    private void logActivity(Authentication authentication, String action, Long targetId, String details) {
        if (authentication != null && authentication.isAuthenticated()) {
            Long adminNo = (Long) authentication.getPrincipal();
            activityLogRepository.save(ActivityLog.builder()
                    .adminNo(adminNo)
                    .action(action)
                    .targetId(targetId)
                    .targetType("product")
                    .details(details)
                    .build());
        }
    }
}
