package com.javajava.project.domain.community.service;

import com.javajava.project.domain.community.dto.NoticeRequestDto;
import com.javajava.project.domain.community.dto.NoticeResponseDto;
import com.javajava.project.domain.community.entity.Notice;
import com.javajava.project.domain.community.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;

    @Override
    public Page<NoticeResponseDto> getNotices(String category, String keyword, Pageable pageable) {
        String cat = (category != null && !category.isBlank() && !"전체".equals(category)) ? category : null;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;
        return noticeRepository.findActiveNotices(cat, kw, pageable)
                .map(NoticeResponseDto::from);
    }

    @Override
    public NoticeResponseDto getNotice(Long noticeNo) {
        Notice notice = noticeRepository.findByNoticeNoAndIsDeleted(noticeNo, 0)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));
        return NoticeResponseDto.from(notice);
    }

    @Override
    public List<NoticeResponseDto> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NoticeResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public Long create(NoticeRequestDto dto, Long adminNo) {
        Notice notice = Notice.builder()
                .adminNo(adminNo)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .isImportant(Boolean.TRUE.equals(dto.getIsImportant()) ? 1 : 0)
                .maintenanceStart(dto.getMaintenanceStart())
                .maintenanceEnd(dto.getMaintenanceEnd())
                .build();
        return noticeRepository.save(notice).getNoticeNo();
    }

    @Override
    @Transactional
    public void update(Long noticeNo, NoticeRequestDto dto) {
        Notice notice = noticeRepository.findById(noticeNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));
        notice.setCategory(dto.getCategory());
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setIsImportant(Boolean.TRUE.equals(dto.getIsImportant()) ? 1 : 0);
        notice.setMaintenanceStart(dto.getMaintenanceStart());
        notice.setMaintenanceEnd(dto.getMaintenanceEnd());
    }

    @Override
    @Transactional
    public void delete(Long noticeNo) {
        Notice notice = noticeRepository.findById(noticeNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));
        notice.setIsDeleted(1);
    }
}
