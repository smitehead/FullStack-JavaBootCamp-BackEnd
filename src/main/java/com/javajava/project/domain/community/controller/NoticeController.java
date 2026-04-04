package com.javajava.project.domain.community.controller;

import com.javajava.project.domain.community.dto.NoticeRequestDto;
import com.javajava.project.domain.community.dto.NoticeResponseDto;
import com.javajava.project.domain.admin.entity.ActivityLog;
import com.javajava.project.domain.admin.repository.ActivityLogRepository;
import com.javajava.project.domain.community.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final ActivityLogRepository activityLogRepository;

    // 일반 유저: 목록 조회 (카테고리 필터 + 검색 + 페이징)
    @GetMapping
    public ResponseEntity<Page<NoticeResponseDto>> getNotices(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(noticeService.getNotices(category, keyword, pageable));
    }

    // 일반 유저: 상세 조회
    @GetMapping("/{noticeNo}")
    public ResponseEntity<NoticeResponseDto> getNotice(@PathVariable("noticeNo") Long noticeNo) {
        return ResponseEntity.ok(noticeService.getNotice(noticeNo));
    }

    // 관리자: 전체 목록 (삭제 포함)
    @GetMapping("/all")
    public ResponseEntity<List<NoticeResponseDto>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    // 관리자: 등록
    @PostMapping
    public ResponseEntity<Long> create(@Valid @RequestBody NoticeRequestDto dto,
                                       Authentication authentication) {
        Long adminNo = (Long) authentication.getPrincipal();
        Long noticeNo = noticeService.create(dto, adminNo);
        logActivity(authentication, "공지 등록", noticeNo, "공지 등록: " + dto.getTitle());
        return ResponseEntity.ok(noticeNo);
    }

    // 관리자: 수정
    @PutMapping("/{noticeNo}")
    public ResponseEntity<Void> update(@PathVariable("noticeNo") Long noticeNo,
                                       @Valid @RequestBody NoticeRequestDto dto,
                                       Authentication authentication) {
        noticeService.update(noticeNo, dto);
        logActivity(authentication, "공지 수정", noticeNo, "공지 수정: " + dto.getTitle());
        return ResponseEntity.ok().build();
    }

    // 관리자: 삭제 (소프트 삭제)
    @DeleteMapping("/{noticeNo}")
    public ResponseEntity<Void> delete(@PathVariable("noticeNo") Long noticeNo,
                                       Authentication authentication) {
        noticeService.delete(noticeNo);
        logActivity(authentication, "공지 삭제", noticeNo, "공지 삭제: #" + noticeNo);
        return ResponseEntity.ok().build();
    }

    private void logActivity(Authentication authentication, String action, Long targetId, String details) {
        if (authentication != null && authentication.isAuthenticated()) {
            Long adminNo = (Long) authentication.getPrincipal();
            activityLogRepository.save(ActivityLog.builder()
                    .adminNo(adminNo)
                    .action(action)
                    .targetId(targetId)
                    .targetType("notice")
                    .details(details)
                    .build());
        }
    }
}
