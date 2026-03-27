package com.javajava.project.controller;

import com.javajava.project.dto.HeroBannerRequestDto;
import com.javajava.project.dto.HeroBannerResponseDto;
import com.javajava.project.entity.ActivityLog;
import com.javajava.project.repository.ActivityLogRepository;
import com.javajava.project.service.HeroBannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class HeroBannerController {

    private final HeroBannerService heroBannerService;
    private final ActivityLogRepository activityLogRepository;

    @GetMapping
    public ResponseEntity<List<HeroBannerResponseDto>> getActiveBanners(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(heroBannerService.getActiveBanners(type));
    }

    @GetMapping("/all")
    public ResponseEntity<List<HeroBannerResponseDto>> getAllBanners() {
        return ResponseEntity.ok(heroBannerService.getAllBanners());
    }

    @PostMapping
    public ResponseEntity<Long> create(@Valid @RequestBody HeroBannerRequestDto dto,
                                        Authentication authentication) {
        Long bannerNo = heroBannerService.create(dto);
        logActivity(authentication, "배너 등록", bannerNo, "배너 등록: " + dto.getImgUrl());
        return ResponseEntity.ok(bannerNo);
    }

    @PutMapping("/{bannerNo}")
    public ResponseEntity<Void> update(@PathVariable Long bannerNo,
                                       @Valid @RequestBody HeroBannerRequestDto dto,
                                       Authentication authentication) {
        heroBannerService.update(bannerNo, dto);
        logActivity(authentication, "배너 수정", bannerNo, "배너 수정: " + dto.getImgUrl());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bannerNo}")
    public ResponseEntity<Void> delete(@PathVariable Long bannerNo,
                                       Authentication authentication) {
        heroBannerService.delete(bannerNo);
        logActivity(authentication, "배너 삭제", bannerNo, "배너 삭제: #" + bannerNo);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{bannerNo}/toggle")
    public ResponseEntity<Void> toggleActive(@PathVariable Long bannerNo,
                                              Authentication authentication) {
        heroBannerService.toggleActive(bannerNo);
        logActivity(authentication, "배너 토글", bannerNo, "배너 활성화/비활성화 토글: #" + bannerNo);
        return ResponseEntity.ok().build();
    }

    private void logActivity(Authentication authentication, String action, Long targetId, String details) {
        if (authentication != null && authentication.isAuthenticated()) {
            Long adminNo = (Long) authentication.getPrincipal();
            activityLogRepository.save(ActivityLog.builder()
                    .adminNo(adminNo)
                    .action(action)
                    .targetId(targetId)
                    .targetType("banner")
                    .details(details)
                    .build());
        }
    }
}
