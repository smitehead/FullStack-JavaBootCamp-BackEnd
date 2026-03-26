package com.javajava.project.service;

import com.javajava.project.dto.ReportResponseDto;
import com.javajava.project.entity.ActivityLog;
import com.javajava.project.entity.Report;
import com.javajava.project.repository.ActivityLogRepository;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;

    @Override
    public List<ReportResponseDto> getAllReports() {
        return enrichWithNicknames(
                reportRepository.findAllByOrderByCreatedAtDesc()
                        .stream()
                        .map(ReportResponseDto::from)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public List<ReportResponseDto> getReportsByStatus(String status) {
        return enrichWithNicknames(
                reportRepository.findByStatusOrderByCreatedAtDesc(status)
                        .stream()
                        .map(ReportResponseDto::from)
                        .collect(Collectors.toList())
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
     * 신고 응답 DTO에 신고자/피신고자 닉네임 세팅
     */
    private List<ReportResponseDto> enrichWithNicknames(List<ReportResponseDto> list) {
        for (ReportResponseDto dto : list) {
            memberRepository.findById(dto.getReporterNo())
                    .ifPresent(m -> dto.setReporterNickname(m.getNickname()));
            if (dto.getTargetMemberNo() != null) {
                memberRepository.findById(dto.getTargetMemberNo())
                        .ifPresent(m -> dto.setTargetMemberNickname(m.getNickname()));
            }
        }
        return list;
    }
}
