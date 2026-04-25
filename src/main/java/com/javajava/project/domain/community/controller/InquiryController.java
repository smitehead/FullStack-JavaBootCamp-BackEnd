package com.javajava.project.domain.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javajava.project.domain.community.dto.InquiryRequestDto;
import com.javajava.project.domain.community.dto.InquiryResponseDto;
import com.javajava.project.domain.community.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final ObjectMapper objectMapper;

    private Long getCurrentMemberNo() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * 문의 등록 (multipart/form-data)
     * Part "data"  : JSON { type, bugType?, title, content }
     * Part "images": 이미지 파일 (선택, 최대 5개)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> create(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {
        InquiryRequestDto dto = objectMapper.readValue(dataJson, InquiryRequestDto.class);
        return ResponseEntity.ok(inquiryService.create(getCurrentMemberNo(), dto, images));
    }

    /** 내 문의 목록 */
    @GetMapping("/my")
    public ResponseEntity<Page<InquiryResponseDto>> getMyInquiries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(inquiryService.getMyInquiries(getCurrentMemberNo(), type, keyword, page, size));
    }

    /** 문의 상세 조회 */
    @GetMapping("/{inquiryNo}")
    public ResponseEntity<InquiryResponseDto> getDetail(@PathVariable Long inquiryNo) {
        return ResponseEntity.ok(inquiryService.getDetail(inquiryNo, getCurrentMemberNo()));
    }
}
