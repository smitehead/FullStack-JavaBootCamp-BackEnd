package com.javajava.project.controller;

import com.javajava.project.dto.HeroBannerRequestDto;
import com.javajava.project.dto.HeroBannerResponseDto;
import com.javajava.project.service.HeroBannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class HeroBannerController {

    private final HeroBannerService heroBannerService;

    /**
     * 활성 배너 목록 조회 (프론트 메인 홈용)
     * GET /api/banners
     * 인증 불필요 - 누구나 접근 가능
     */
    @GetMapping
    public ResponseEntity<List<HeroBannerResponseDto>> getActiveBanners() {
        return ResponseEntity.ok(heroBannerService.getActiveBanners());
    }

    /**
     * 배너 등록 (관리자)
     * POST /api/banners
     * Body: { imgUrl, linkUrl, sortOrder, isActive, endAt }
     */
    @PostMapping
    public ResponseEntity<Long> create(@Valid @RequestBody HeroBannerRequestDto dto) {
        return ResponseEntity.ok(heroBannerService.create(dto));
    }

    /**
     * 배너 수정 (관리자)
     * PUT /api/banners/{bannerNo}
     * Body: { imgUrl, linkUrl, sortOrder, isActive, endAt }
     */
    @PutMapping("/{bannerNo}")
    public ResponseEntity<Void> update(@PathVariable Long bannerNo,
                                       @Valid @RequestBody HeroBannerRequestDto dto) {
        heroBannerService.update(bannerNo, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * 배너 삭제 (관리자)
     * DELETE /api/banners/{bannerNo}
     */
    @DeleteMapping("/{bannerNo}")
    public ResponseEntity<Void> delete(@PathVariable Long bannerNo) {
        heroBannerService.delete(bannerNo);
        return ResponseEntity.ok().build();
    }
}
