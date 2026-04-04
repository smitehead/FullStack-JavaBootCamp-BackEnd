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

        if ("거절".equals(action)) {
            withdraw.setRejectReason(rejectReason);

            // 거절 시 포인트 환불 (비관적 락)
            Member member = memberRepository.findByIdWithLock(withdraw.getMemberNo())
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
            long refundedBalance = member.getPoints() + withdraw.getAmount();
            member.setPoints(refundedBalance);

            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(withdraw.getMemberNo())
                    .type("출금거절환불")
                    .amount(withdraw.getAmount())
                    .balance(refundedBalance)
                    .reason("출금 신청 거절로 인한 환불 (관리자: " + adminNickname + ")")
                    .build());

            try { sseService.sendPointUpdate(withdraw.getMemberNo(), refundedBalance); }
            catch (Exception e) { log.warn("[WithdrawAdmin] SSE 실패"); }
        }

        log.info("[WithdrawAdmin] 처리 완료. withdrawNo={}, action={}, admin={}",
                withdrawNo, action, adminNickname);
    }
}