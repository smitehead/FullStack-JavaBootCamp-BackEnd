package com.javajava.project.domain.admin.service;

import com.javajava.project.domain.admin.dto.WithdrawAdminResponseDto;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.point.entity.PointWithdraw;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.point.repository.PointWithdrawRepository;
import com.javajava.project.global.sse.SseService;
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
public class WithdrawAdminServiceImpl implements WithdrawAdminService {

    private final PointWithdrawRepository pointWithdrawRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SseService sseService;

    @Override
    @Transactional(readOnly = true)
    public Page<WithdrawAdminResponseDto> getWithdrawList(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PointWithdraw> withdrawPage = "전체".equals(status)
                ? pointWithdrawRepository.findAllByOrderByCreatedAtDesc(pageable)
                : pointWithdrawRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        return withdrawPage.map(w -> {
            String nickname = memberRepository.findById(w.getMemberNo())
                    .map(Member::getNickname).orElse("알 수 없음");
            return WithdrawAdminResponseDto.from(w, nickname);
        });
    }

    @Override
    @Transactional
    public void processWithdraw(Long withdrawNo, String action,
                                 Long adminNo, String adminNickname, String rejectReason) {
        PointWithdraw withdraw = pointWithdrawRepository.findById(withdrawNo)
                .orElseThrow(() -> new IllegalArgumentException("출금 신청을 찾을 수 없습니다."));

        if ("완료".equals(withdraw.getStatus()) || "거절".equals(withdraw.getStatus()))
            throw new IllegalStateException("이미 처리된 출금 신청입니다.");

        withdraw.setStatus(action);
        withdraw.setAdminNo(adminNo);
        withdraw.setAdminNickname(adminNickname);
        withdraw.setProcessedAt(LocalDateTime.now());

        if ("완료".equals(action)) {
        // 완료 처리 시 포인트 차감 + 이력 기록
        Member member = memberRepository.findByIdWithLock(withdraw.getMemberNo())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getPoints() < withdraw.getAmount())
            throw new IllegalStateException("포인트가 부족합니다. 회원에게 확인이 필요합니다.");

        long newBalance = member.getPoints() - withdraw.getAmount();
        member.setPoints(newBalance);

        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(withdraw.getMemberNo())
                .type("출금")
                .amount(-withdraw.getAmount())
                .balance(newBalance)
                .reason("포인트 출금 완료 (" + withdraw.getBankName() + " " + withdraw.getAccountNumber() + ")")
                .build());

        try { sseService.sendPointUpdate(withdraw.getMemberNo(), newBalance); }
        catch (Exception e) { log.warn("[WithdrawAdmin] SSE 실패"); }

    } else if ("거절".equals(action)) {
        withdraw.setRejectReason(rejectReason);
        // 거절은 포인트 차감 안 했으므로 환불도 없음
    }

    log.info("[WithdrawAdmin] 처리 완료. withdrawNo={}, action={}, admin={}",
            withdrawNo, action, adminNickname);
    }
}