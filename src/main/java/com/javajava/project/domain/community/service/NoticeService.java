package com.javajava.project.domain.community.service;

import com.javajava.project.domain.community.dto.NoticeRequestDto;
import com.javajava.project.domain.community.dto.NoticeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoticeService {

    // 일반 유저: 목록 조회 (카테고리 필터 + 검색 + 페이징)
    Page<NoticeResponseDto> getNotices(String category, String keyword, Pageable pageable);

    // 일반 유저: 상세 조회
    NoticeResponseDto getNotice(Long noticeNo);

    // 관리자: 전체 목록 (삭제 포함)
    List<NoticeResponseDto> getAllNotices();

    // 관리자: 등록
    Long create(NoticeRequestDto dto, Long adminNo);

    // 관리자: 수정
    void update(Long noticeNo, NoticeRequestDto dto);

    // 관리자: 삭제 (소프트 삭제)
    void delete(Long noticeNo);
}
