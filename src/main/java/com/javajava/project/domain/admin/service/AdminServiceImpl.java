package com.javajava.project.domain.admin.service;

import com.javajava.project.domain.admin.dto.ActivityLogResponseDto;
import com.javajava.project.domain.admin.dto.AdminMemberResponseDto;
import com.javajava.project.domain.member.dto.MannerHistoryResponseDto;
import com.javajava.project.domain.admin.entity.ActivityLog;
import com.javajava.project.domain.member.entity.MannerHistory;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.admin.repository.ActivityLogRepository;
import com.javajava.project.domain.member.repository.MannerHistoryRepository;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.product.repository.ProductRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final MemberRepository memberRepository;
    private final MannerHistoryRepository mannerHistoryRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;
    private final PointHistoryRepository pointHistoryRepository;
    private final SseService sseService;
    private final ProductRepository productRepository;

    @Override
    public List<ActivityLogResponseDto> getAllActivityLogs() {
        List<ActivityLogResponseDto> list = activityLogRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(ActivityLogResponseDto::from)
                .toList();
        enrichWithAdminNicknames(list);
        return list;
    }

    @Override
    public List<ActivityLogResponseDto> getActivityLogsByTargetType(String targetType) {
        List<ActivityLogResponseDto> list = activityLogRepository
                .findByTargetTypeOrderByCreatedAtDesc(targetType)
                .stream()
                .map(ActivityLogResponseDto::from)
                .toList();
        enrichWithAdminNicknames(list);
        return list;
    }

    private void enrichWithAdminNicknames(List<ActivityLogResponseDto> list) {
        Set<Long> adminNos = list.stream()
                .map(ActivityLogResponseDto::getAdminNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = memberRepository.findAllById(adminNos).stream()
                .collect(Collectors.toMap(Member::getMemberNo, Member::getNickname));
        list.forEach(dto -> dto.setAdminNickname(nicknameMap.get(dto.getAdminNo())));
    }

    @Override
    public List<AdminMemberResponseDto> getAllMembers() {
        return memberRepository.findAllByOrderByJoinedAtDesc()
                .stream()
                .map(m -> {
                    AdminMemberResponseDto dto = AdminMemberResponseDto.from(m);
                    dto.setPostCount(productRepository.countBySellerNoAndIsDeleted(m.getMemberNo(), 0));
                    return dto;
                })
                .toList();
    }

    @Override
    public List<AdminMemberResponseDto> searchMembers(String keyword) {
        return memberRepository.searchByKeyword(keyword)
                .stream()
                .map(m -> {
                    AdminMemberResponseDto dto = AdminMemberResponseDto.from(m);
                    dto.setPostCount(productRepository.countBySellerNoAndIsDeleted(m.getMemberNo(), 0));
                    return dto;
                })
                .toList();
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
        // 비관적 락을 사용하여 회원 정보 조회
        Member member = memberRepository.findByIdWithLock(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        long previousPoints = member.getPoints();
        long newBalance = previousPoints + pointAmount;
        member.setPoints(newBalance);

        // 포인트 이력(PointHistory) 기록
        String type = pointAmount >= 0 ? "관리자추가" : "관리자회수";
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(memberNo)
                .type(type)
                .amount(pointAmount)
                .balance(newBalance)
                .reason("관리자에 의한 포인트 조정 (" + (pointAmount >= 0 ? "+" : "") + pointAmount + "P)")
                .build());

        // 실시간 포인트 반영 (SSE)
        try {
            sseService.sendPointUpdate(memberNo, newBalance);
        } catch (Exception e) {
            // SSE 오류가 전체 트랜잭션에 영향을 주지 않도록 예외 처리
        }

        // 활동 로그 기록
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
                .toList();
        enrichWithMemberNicknames(list);
        return list;
    }

    @Override
    public List<MannerHistoryResponseDto> getMannerHistoryByMember(Long memberNo) {
        List<MannerHistoryResponseDto> list = mannerHistoryRepository.findByMemberNoOrderByCreatedAtDesc(memberNo)
                .stream()
                .map(MannerHistoryResponseDto::from)
                .toList();
        enrichWithMemberNicknames(list);
        return list;
    }

    private void enrichWithMemberNicknames(List<MannerHistoryResponseDto> list) {
        Set<Long> ids = list.stream()
                .flatMap(dto -> {
                    Set<Long> nos = new java.util.HashSet<>();
                    if (dto.getMemberNo() != null) nos.add(dto.getMemberNo());
                    if (dto.getAdminNo() != null) nos.add(dto.getAdminNo());
                    return nos.stream();
                })
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getMemberNo, Member::getNickname));
        list.forEach(dto -> {
            dto.setMemberNickname(nicknameMap.get(dto.getMemberNo()));
            if (dto.getAdminNo() != null) dto.setAdminNickname(nicknameMap.get(dto.getAdminNo()));
        });
    }
}
