package com.javajava.project.domain.member.scheduler;

import com.javajava.project.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberScheduler {

    private static final int ARCHIVE_AFTER_DAYS = 30;

    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void archiveWithdrawnMembers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ARCHIVE_AFTER_DAYS);

        int archived = memberRepository.insertWithdrawnToArchive(cutoff);
        if (archived == 0) return;

        int deleted = memberRepository.deleteArchivedMembers(cutoff);
        log.info("[MemberScheduler] 탈퇴 회원 아카이브 완료: {}명 이동, {}명 삭제", archived, deleted);
    }
}
