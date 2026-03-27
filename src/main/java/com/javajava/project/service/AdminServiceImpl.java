package com.javajava.project.service;

import com.javajava.project.dto.ActivityLogResponseDto;
import com.javajava.project.dto.AdminMemberResponseDto;
import com.javajava.project.dto.MannerHistoryResponseDto;
import com.javajava.project.entity.ActivityLog;
import com.javajava.project.entity.MannerHistory;
import com.javajava.project.entity.Member;
import com.javajava.project.repository.ActivityLogRepository;
import com.javajava.project.repository.MannerHistoryRepository;
import com.javajava.project.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final MemberRepository memberRepository;
    private final MannerHistoryRepository mannerHistoryRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;

    @Override
    public List<ActivityLogResponseDto> getAllActivityLogs() {
        List<ActivityLogResponseDto> list = activityLogRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(ActivityLogResponseDto::from)
                .collect(Collectors.toList());

        for (ActivityLogResponseDto dto : list) {
            memberRepository.findById(dto.getAdminNo())
                    .ifPresent(m -> dto.setAdminNickname(m.getNickname()));
        }
        return list;
    }

    @Override
    public List<ActivityLogResponseDto> getActivityLogsByTargetType(String targetType) {
        List<ActivityLogResponseDto> list = activityLogRepository.findByTargetTypeOrderByCreatedAtDesc(targetType)
                .stream()
                .map(ActivityLogResponseDto::from)
                .collect(Collectors.toList());

        for (ActivityLogResponseDto dto : list) {
            memberRepository.findById(dto.getAdminNo())
                    .ifPresent(m -> dto.setAdminNickname(m.getNickname()));
        }
        return list;
    }

    @Override
    public List<AdminMemberResponseDto> getAllMembers() {
        return memberRepository.findAllByOrderByJoinedAtDesc()
                .stream()
                .map(AdminMemberResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminMemberResponseDto> searchMembers(String keyword) {
        return memberRepository.searchByKeyword(keyword)
                .stream()
                .map(AdminMemberResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void suspendMember(Long memberNo, Integer suspendDays, String suspendReason, Long adminNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        boolean isPermanent = suspendDays >= 999;

        member.setIsSuspended(1);
        member.setSuspendReason(suspendReason);

        if (isPermanent) {
            member.setIsPermanentSuspended(1);
            member.setSuspendUntil(null); // 영구정지는 종료일 없음
        } else {
            member.setIsPermanentSuspended(0);
            member.setSuspendUntil(LocalDateTime.now().plusDays(suspendDays));
        }

        // 활동 로그 기록
        String detail = isPermanent
                ? member.getNickname() + "님 영구정지 처리: " + suspendReason
                : member.getNickname() + "님 정지 처리 (" + suspendDays + "일): " + suspendReason;

        activityLogRepository.save(ActivityLog.builder()
                .adminNo(adminNo)
                .action("사용자 정지")
                .targetId(memberNo)
                .targetType("user")
                .details(detail)
                .build());

        // 알림 발송
        String notiContent = isPermanent
                ? "계정이 영구 정지되었습니다. 사유: " + suspendReason
                : "계정이 " + suspendDays + "일간 정지되었습니다. 사유: " + suspendReason;

        notificationService.sendAndSaveNotification(memberNo, "제재", notiContent, null);
    }

    @Override
    @Transactional
    public void unsuspendMember(Long memberNo, Long adminNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        member.setIsSuspended(0);
        member.setSuspendUntil(null);
        member.setSuspendReason(null);
        member.setIsPermanentSuspended(0);

        // 활동 로그
        activityLogRepository.save(ActivityLog.builder()
                .adminNo(adminNo)
                .action("정지 해제")
                .targetId(memberNo)
                .targetType("user")
                .details(member.getNickname() + "님 정지 해제")
                .build());

        // 알림 발송
        notificationService.sendAndSaveNotification(memberNo, "제재해제", "계정 정지가 해제되었습니다.", null);
    }

    @Override
    @Transactional
    public void updateMannerTemp(Long memberNo, Double newTemp, String reason, Long adminNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Double previousTemp = member.getMannerTemp();
        member.setMannerTemp(newTemp);

        // 매너온도 변동 이력 저장
        mannerHistoryRepository.save(MannerHistory.builder()
                .memberNo(memberNo)
                .previousTemp(previousTemp)
                .newTemp(newTemp)
                .reason(reason)
                .adminNo(adminNo)
                .build());

        // 활동 로그
        activityLogRepository.save(ActivityLog.builder()
                .adminNo(adminNo)
                .action("매너온도 변경")
                .targetId(memberNo)
                .targetType("user")
                .details(member.getNickname() + "님 매너온도 변경: " + previousTemp + " → " + newTemp)
                .build());
    }

    @Override
    @Transactional
    public void updatePoints(Long memberNo, Long pointAmount, Long adminNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        member.setPoints(member.getPoints() + pointAmount);

        // 활동 로그
        String sign = pointAmount >= 0 ? "+" : "";
        activityLogRepository.save(ActivityLog.builder()
                .adminNo(adminNo)
                .action("포인트 변경")
                .targetId(memberNo)
                .targetType("user")
                .details(member.getNickname() + "님 포인트 변경: " + sign + pointAmount + "P")
                .build());
    }

    @Override
    @Transactional
    public void updateRole(Long memberNo, Integer isAdmin, Long adminNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        member.setIsAdmin(isAdmin);

        // 활동 로그
        String roleName = isAdmin == 1 ? "관리자" : "일반";
        activityLogRepository.save(ActivityLog.builder()
                .adminNo(adminNo)
                .action("권한 변경")
                .targetId(memberNo)
                .targetType("user")
                .details(member.getNickname() + "님 권한 변경: " + roleName)
                .build());
    }

    @Override
    public List<MannerHistoryResponseDto> getAllMannerHistory() {
        List<MannerHistoryResponseDto> list = mannerHistoryRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(MannerHistoryResponseDto::from)
                .collect(Collectors.toList());

        // 닉네임 세팅
        for (MannerHistoryResponseDto dto : list) {
            memberRepository.findById(dto.getMemberNo())
                    .ifPresent(m -> dto.setMemberNickname(m.getNickname()));
            if (dto.getAdminNo() != null) {
                memberRepository.findById(dto.getAdminNo())
                        .ifPresent(m -> dto.setAdminNickname(m.getNickname()));
            }
        }
        return list;
    }

    @Override
    public List<MannerHistoryResponseDto> getMannerHistoryByMember(Long memberNo) {
        List<MannerHistoryResponseDto> list = mannerHistoryRepository.findByMemberNoOrderByCreatedAtDesc(memberNo)
                .stream()
                .map(MannerHistoryResponseDto::from)
                .collect(Collectors.toList());

        for (MannerHistoryResponseDto dto : list) {
            memberRepository.findById(dto.getMemberNo())
                    .ifPresent(m -> dto.setMemberNickname(m.getNickname()));
            if (dto.getAdminNo() != null) {
                memberRepository.findById(dto.getAdminNo())
                        .ifPresent(m -> dto.setAdminNickname(m.getNickname()));
            }
        }
        return list;
    }
}
