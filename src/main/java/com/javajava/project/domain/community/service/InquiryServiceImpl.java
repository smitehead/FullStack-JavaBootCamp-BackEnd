package com.javajava.project.domain.community.service;

import com.javajava.project.domain.admin.entity.ActivityLog;
import com.javajava.project.domain.admin.repository.ActivityLogRepository;
import com.javajava.project.domain.community.dto.InquiryAnswerDto;
import com.javajava.project.domain.community.dto.InquiryRequestDto;
import com.javajava.project.domain.community.dto.InquiryResponseDto;
import com.javajava.project.domain.community.entity.Inquiry;
import com.javajava.project.domain.community.entity.InquiryImage;
import com.javajava.project.domain.community.repository.InquiryImageRepository;
import com.javajava.project.domain.community.repository.InquiryRepository;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.global.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final MemberRepository memberRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;
    private final FileStore fileStore;

    @Override
    @Transactional
    public Long create(Long memberNo, InquiryRequestDto dto, List<MultipartFile> images) {
        Inquiry inquiry = Inquiry.builder()
                .memberNo(memberNo)
                .type(dto.getType())
                .bugType(dto.getBugType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();
        inquiryRepository.save(inquiry);

        // 첨부 이미지 저장
        if (images != null) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                try {
                    FileStore.StoredImage stored = fileStore.storeImageFile(file);
                    inquiryImageRepository.save(InquiryImage.builder()
                            .inquiryNo(inquiry.getInquiryNo())
                            .originalName(stored.originalName())
                            .uuidName(stored.uuidName())
                            .imagePath(stored.imagePath())
                            .build());
                } catch (Exception e) {
                    log.warn("[Inquiry] 이미지 저장 실패. inquiryNo={}, file={}", inquiry.getInquiryNo(), file.getOriginalFilename(), e);
                }
            }
        }

        return inquiry.getInquiryNo();
    }

    @Override
    public Page<InquiryResponseDto> getMyInquiries(Long memberNo, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return inquiryRepository.findByMemberNoOrderByCreatedAtDesc(memberNo, pageable)
                .map(i -> enrichWithImages(InquiryResponseDto.from(i,
                        memberRepository.findById(i.getMemberNo())
                                .map(Member::getNickname).orElse("알 수 없음"))));
    }

    @Override
    public InquiryResponseDto getDetail(Long inquiryNo, Long memberNo) {
        Inquiry inquiry = inquiryRepository.findById(inquiryNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));
        String nick = memberRepository.findById(inquiry.getMemberNo())
                .map(Member::getNickname).orElse("알 수 없음");
        return enrichWithImages(InquiryResponseDto.from(inquiry, nick));
    }

    @Override
    public Page<InquiryResponseDto> getAll(Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Inquiry> result = (status == null)
                ? inquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
                : inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return result.map(i -> enrichWithImages(InquiryResponseDto.from(i,
                memberRepository.findById(i.getMemberNo())
                        .map(Member::getNickname).orElse("알 수 없음"))));
    }

    /** 문의 응답 DTO에 첨부 이미지 URL 세팅 */
    private InquiryResponseDto enrichWithImages(InquiryResponseDto dto) {
        List<String> urls = inquiryImageRepository.findByInquiryNo(dto.getInquiryNo())
                .stream()
                .map(img -> "/api/images/" + img.getUuidName())
                .toList();
        dto.setImageUrls(urls);
        return dto;
    }

    @Override
    @Transactional
    public void answer(Long inquiryNo, Long adminNo, String adminNickname, InquiryAnswerDto dto) {
        Inquiry inquiry = inquiryRepository.findById(inquiryNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        inquiry.setAnswer(dto.getAnswer());
        inquiry.setStatus(1);
        inquiry.setAnsweredAt(LocalDateTime.now());
        inquiry.setAdminNo(adminNo);
        inquiry.setAdminNickname(adminNickname);

        // 사용자에게 답변 완료 알림 전송
        try {
            notificationService.sendAndSaveNotification(
                    inquiry.getMemberNo(),
                    "activity",
                    "문의하신 '" + inquiry.getTitle() + "'에 답변이 등록되었습니다.",
                    "/inquiry/" + inquiryNo);
        } catch (Exception e) {
            log.warn("[Inquiry] 알림 전송 실패. inquiryNo={}", inquiryNo);
        }

        // 관리자 활동 로그 기록
        activityLogRepository.save(ActivityLog.builder()
                .adminNo(adminNo)
                .action("1:1 문의 답변 등록")
                .targetId(inquiryNo)
                .targetType("inquiry")
                .details("문의 제목: " + inquiry.getTitle())
                .build());

        log.info("[Inquiry] 답변 등록 및 로그 기록. inquiryNo={}, adminNo={}", inquiryNo, adminNo);
    }

    @Override
    @Transactional
    public void delete(Long inquiryNo) {
        inquiryRepository.deleteById(inquiryNo);
    }
}
