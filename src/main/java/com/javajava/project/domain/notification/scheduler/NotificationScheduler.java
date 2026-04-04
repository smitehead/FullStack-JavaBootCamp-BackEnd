package com.javajava.project.domain.notification.scheduler;

import com.javajava.project.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;

    /**
     * 매일 새벽 3시에 실행되어 30일이 지난 오래된 알림 기록을 자동 삭제합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoffDate);
        
        log.info("[Scheduler] 30일이 지난 오래된 알림 데이터 {}건을 성공적으로 삭제했습니다. (기준일: {})", deletedCount, cutoffDate);
    }
}