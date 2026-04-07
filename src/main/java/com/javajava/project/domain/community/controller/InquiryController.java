package com.javajava.project.domain.community.controller;

import com.javajava.project.domain.community.dto.InquiryRequestDto;
import com.javajava.project.domain.community.dto.InquiryResponseDto;
import com.javajava.project.domain.community.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    private Long getCurrentMemberNo() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** 문의 등록 */
    @PostMapping
    public ResponseEntity<Long> create(@Valid @RequestBody InquiryRequestDto dto) {
        return ResponseEntity.ok(inquiryService.create(getCurrentMemberNo(), dto));
    }

    /** 내 문의 목록 */
    @GetMapping("/my")
    public ResponseEntity<Page<InquiryResponseDto>> getMyInquiries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inquiryService.getMyInquiries(getCurrentMemberNo(), page, size));
    }

    /** 문의 상세 조회 */
    @GetMapping("/{inquiryNo}")
    public ResponseEntity<InquiryResponseDto> getDetail(@PathVariable Long inquiryNo) {
        return ResponseEntity.ok(inquiryService.getDetail(inquiryNo, getCurrentMemberNo()));
    }
}