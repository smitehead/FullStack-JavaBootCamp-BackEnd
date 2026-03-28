package com.javajava.project.controller;

import com.javajava.project.dto.AdminProductResponseDto;
import com.javajava.project.entity.ActivityLog;
import com.javajava.project.repository.ActivityLogRepository;
import com.javajava.project.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
