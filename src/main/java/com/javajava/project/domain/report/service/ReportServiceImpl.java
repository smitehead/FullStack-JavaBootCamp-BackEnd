package com.javajava.project.domain.report.service;

import com.javajava.project.domain.report.dto.ReportRequestDto;
import com.javajava.project.domain.report.dto.ReportResponseDto;
import com.javajava.project.domain.admin.entity.ActivityLog;
import com.javajava.project.domain.report.entity.Report;
import com.javajava.project.domain.report.entity.ReportImage;
import com.javajava.project.domain.admin.repository.ActivityLogRepository;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.report.repository.ReportImageRepository;
import com.javajava.project.domain.report.repository.ReportRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.global.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportImageRepository reportImageRepository;
    private final MemberRepository memberRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;
    private final FileStore fileStore;

    @Override
    @Transactional
    public Long submitReport(ReportRequestDto dto, List<MultipartFile> images) {
        Report saved = reportRepository.save(Report.builder()
                .reporterNo(dto.getReporterNo())
                .targetMemberNo(dto.getTargetMemberNo())
                .targetProductNo(dto.getTargetProductNo())
                .type(dto.getType())
                .content(dto.getContent())
                .build());

        // 첨부 이미지 저장
        if (images != null) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                try {
                    FileStore.StoredImage stored = fileStore.storeImageFile(file);
                    reportImageRepository.save(ReportImage.builder()
                            .reportNo(saved.getReportNo())
                            .originalName(stored.originalName())
                            .uuidName(stored.uuidName())
                            .imagePath(stored.imagePath())
                            .build());
                } catch (Exception e) {
                    log.warn("[Report] 이미지 저장 실패. reportNo={}, file={}", saved.getReportNo(), file.getOriginalFilename(), e);
                }
            }
        }

        return saved.getReportNo();
    }

    @Override
    public ReportResponseDto getReportDetail(Long reportNo) {
        Report report = reportRepository.findById(reportNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));
        ReportResponseDto dto = ReportResponseDto.from(report);
        memberRepository.findById(dto.getReporterNo())
                .ifPresent(m -> dto.setReporterNickname(m.getNickname()));
        if (dto.getTargetMemberNo() != null) {
            memberRepository.findById(dto.getTargetMemberNo())
                    .ifPresent(m -> dto.setTargetMemberNickname(m.getNickname()));
        }
        List<String> urls = reportImageRepository.findByReportNo(reportNo)
                .stream()
                .map(img -> "/api/images/" + img.getUuidName())
                .toList();
        dto.setImageUrls(urls);
        return dto;
    }

    @Override
    public List<ReportResponseDto> getAllReports() {
        return enrichWithNicknames(
                reportRepository.findAllByOrderByCreatedAtDesc()
                        .stream()
                        .map(ReportResponseDto::from)
                        .toList()
        );
    }

    @Override
    public List<ReportResponseDto> getReportsByStatus(String status) {
        return enrichWithNicknames(
                reportRepository.findByStatusOrderByCreatedAtDesc(status)
                        .stream()
                        .map(ReportResponseDto::from)
                        .toList()
        );
    }

    @Override
    @Transactional
    public void resolveReport(Long reportNo, String status, String penaltyMsg, Long adminNo) {
        Report report = reportRepository.findById(reportNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));

        report.setStatus(status);
        if (penaltyMsg != null) {
            report.setPenaltyMsg(penaltyMsg);
        }

        // 활동 로그
        activityLogRepository.save(ActivityLog.builder()
                .adminNo(adminNo)
                .action("신고 처리")
                .targetId(reportNo)
                .targetType("report")
                .details("신고 처리: #" + reportNo + " (" + status + ")")
                .build());

        // 신고자에게 처리 결과 알림
        notificationService.sendAndSaveNotification(
                report.getReporterNo(),
                "신고처리",
                "접수하신 신고(#" + reportNo + ")가 " + status + " 처리되었습니다.",
                null
        );

        // 피신고자에게도 제재 알림 (제재 내용이 있는 경우)
        if (penaltyMsg != null && report.getTargetMemberNo() != null) {
            notificationService.sendAndSaveNotification(
                    report.getTargetMemberNo(),
                    "제재",
                    "신고 처리에 따른 제재 안내: " + penaltyMsg,
                    null
            );
        }
    }

    /**
     * 신고 응답 DTO에 신고자/피신고자 닉네임 + 첨부 이미지 URL 세팅
     */
    private List<ReportResponseDto> enrichWithNicknames(List<ReportResponseDto> list) {
        for (ReportResponseDto dto : list) {
            memberRepository.findById(dto.getReporterNo())
                    .ifPresent(m -> dto.setReporterNickname(m.getNickname()));
            if (dto.getTargetMemberNo() != null) {
                memberRepository.findById(dto.getTargetMemberNo())
                        .ifPresent(m -> dto.setTargetMemberNickname(m.getNickname()));
            }
            // 첨부 이미지 URL 세팅
            List<String> urls = reportImageRepository.findByReportNo(dto.getReportNo())
                    .stream()
                    .map(img -> "/api/images/" + img.getUuidName())
                    .toList();
            dto.setImageUrls(urls);
        }
        return list;
    }
}
