package com.javajava.project.domain.community.service;

import com.javajava.project.domain.community.dto.InquiryAnswerDto;
import com.javajava.project.domain.community.dto.InquiryRequestDto;
import com.javajava.project.domain.community.dto.InquiryResponseDto;
import com.javajava.project.domain.community.entity.Inquiry;
import com.javajava.project.domain.community.repository.InquiryRepository;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Long create(Long memberNo, InquiryRequestDto dto) {
        Inquiry inquiry = Inquiry.builder()
                .memberNo(memberNo)
                .type(dto.getType())
                .bugType(dto.getBugType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();
        inquiryRepository.save(inquiry);
        return inquiry.getInquiryNo();
    }

    @Override
    public Page<InquiryResponseDto> getMyInquiries(Long memberNo, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return inquiryRepository.findByMemberNoOrderByCreatedAtDesc(memberNo, pageable)
                .map(i -> {
                    String nick = memberRepository.findById(i.getMemberNo())
                            .map(Member::getNickname).orElse("알 수 없음");
                    return InquiryResponseDto.from(i, nick);
                });
    }

    @Override
    public InquiryResponseDto getDetail(Long inquiryNo, Long memberNo) {
        Inquiry inquiry = inquiryRepository.findById(inquiryNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));
        // 본인 문의 또는 관리자만 조회 가능 — 컨트롤러에서 별도 처리 가능
        String nick = memberRepository.findById(inquiry.getMemberNo())
                .map(Member::getNickname).orElse("알 수 없음");
        return InquiryResponseDto.from(inquiry, nick);
    }

    @Override
    public Page<InquiryResponseDto> getAll(Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Inquiry> result = (status == null)
                ? inquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
                : inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return result.map(i -> {
            String nick = memberRepository.findById(i.getMemberNo())
                    .map(Member::getNickname).orElse("알 수 없음");
            return InquiryResponseDto.from(i, nick);
        });
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
                    "/inquiry/" + inquiryNo
            );
        } catch (Exception e) {
            log.warn("[Inquiry] 알림 전송 실패. inquiryNo={}", inquiryNo);
        }

        log.info("[Inquiry] 답변 등록. inquiryNo={}, adminNo={}", inquiryNo, adminNo);
    }

    @Override
    @Transactional
    public void delete(Long inquiryNo) {
        inquiryRepository.deleteById(inquiryNo);
    }
}