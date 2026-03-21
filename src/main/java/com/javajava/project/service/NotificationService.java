package com.javajava.project.service;

import com.javajava.project.dto.NotificationResponseDto;
import com.javajava.project.entity.Notification;
import com.javajava.project.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseService sseService;

    /**
     * 알림을 생성하여 DB에 저장하고, 사용자가 접속 중이라면 SSE로 실시간 전송합니다.
     */
    @Transactional
    public void sendAndSaveNotification(Long memberNo, String type, String content, String linkUrl) {
        // 1. 데이터베이스에 알림 내역 저장
        Notification notification = Notification.builder()
                .memberNo(memberNo)
                .type(type)
                .content(content)
                .linkUrl(linkUrl)
                .isRead(0)
                .build();
        
        notificationRepository.save(notification);

        // 2. 현재 로그인(접속) 중인 클라이언트에게 실시간 알림 발송
        // 프론트엔드에서는 이 데이터를 받아 우측 하단에 Toast 알림으로 띄우면 됩니다.
        sseService.sendToClient(String.valueOf(memberNo), content);
    }

    /**
     * 특정 회원의 알림 목록 조회 (최신순)
     */
    public List<NotificationResponseDto> getNotifications(Long memberNo) {
        return notificationRepository.findByMemberNoOrderByCreatedAtDesc(memberNo)
                .stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 미읽음 알림 개수 조회 (헤더 뱃지용)
     */
    public long getUnreadCount(Long memberNo) {
        return notificationRepository.countByMemberNoAndIsRead(memberNo, 0);
    }

    /**
     * 알림 읽음 처리 (알림을 클릭해서 이동할 때 호출)
     */
    @Transactional
    public void markAsRead(Long notiNo) {
        Notification notification = notificationRepository.findById(notiNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));
        notification.setIsRead(1);
    }

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long memberNo) {
        notificationRepository.markAllAsRead(memberNo);
    }
}