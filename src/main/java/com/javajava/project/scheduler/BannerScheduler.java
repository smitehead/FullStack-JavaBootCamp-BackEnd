package com.javajava.project.scheduler;

import com.javajava.project.entity.HeroBanner;
import com.javajava.project.repository.HeroBannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BannerScheduler {

    private final HeroBannerRepository heroBannerRepository;

    // 매 1분마다 실행 — endAt이 지난 활성 배너를 자동 비활성화
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deactivateExpiredBanners() {
        List<HeroBanner> all = heroBannerRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        int count = 0;
        for (HeroBanner banner : all) {
            if (banner.getIsActive() == 1 && banner.getEndAt() != null && banner.getEndAt().isBefore(now)) {
                banner.setIsActive(0);
                count++;
            }
        }

        if (count > 0) {
            log.info("만료 배너 {}개 비활성화 처리 완료", count);
        }
    }
}
