package com.javajava.project.domain.community.service;

import com.javajava.project.domain.community.dto.InquiryAnswerDto;
import com.javajava.project.domain.community.dto.InquiryRequestDto;
import com.javajava.project.domain.community.dto.InquiryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InquiryService {
    Long create(Long memberNo, InquiryRequestDto dto, List<MultipartFile> images);
    Page<InquiryResponseDto> getMyInquiries(Long memberNo, String type, String keyword, int page, int size);
    InquiryResponseDto getDetail(Long inquiryNo, Long memberNo);
    // 관리자
    Page<InquiryResponseDto> getAll(Integer status, int page, int size);
    void answer(Long inquiryNo, Long adminNo, String adminNickname, InquiryAnswerDto dto);
    void delete(Long inquiryNo);
}