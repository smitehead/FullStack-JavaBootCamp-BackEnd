package com.javajava.project.domain.notification.service;

import com.javajava.project.domain.notification.dto.NotificationResponseDto;
import com.javajava.project.domain.notification.entity.Notification;
import com.javajava.project.domain.notification.repository.NotificationRepository;
import com.javajava.project.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

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
        
        Notification saved = notificationRepository.save(notification);

        // 2. 현재 로그인(접속) 중인 클라이언트에게 실시간 알림 발송
        // 프론트엔드: source.addEventListener('notification', e => { const d = JSON.parse(e.data); ... })
        // d.type으로 알림 종류 구분, d.linkUrl로 클릭 시 이동 처리
        sseService.sendToClient(String.valueOf(memberNo), Map.of(
                "notiNo",    saved.getNotiNo(),
                "type",      type,
                "content",   content,
                "linkUrl",   linkUrl != null ? linkUrl : "",
                "isRead",    0,
                "createdAt", saved.getCreatedAt().toString()
        ));
    }

    /**
     * 특정 회원의 알림 목록 조회 (최신순)
     */
    public List<NotificationResponseDto> getNotifications(Long memberNo) {
        return notificationRepository.findByMemberNoOrderByCreatedAtDesc(memberNo)
                .stream()
                .map(NotificationResponseDto::from)
                .toList();
    }

    /**
     * 최근 알림 조회 (관리자 대시보드용, 전체 회원 대상)
     */
    public List<NotificationResponseDto> getAllRecentNotifications(int limit) {
        return notificationRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).stream()
                .map(NotificationResponseDto::from)
                .toList();
    }

    /**
     * 관리자 발송 알림 조회 (중복 제거, 타입 필터 지원)
     * type이 null이거나 "all"이면 전체 관리자 타입 조회
     */
    public List<NotificationResponseDto> getAdminBroadcasts(String type, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<String> adminTypes = List.of("시스템", "활동", "입찰");

        List<Notification> notifications = (type != null && !type.equals("all") && adminTypes.contains(type))
                ? notificationRepository.findDistinctAdminBroadcastsByType(type, pageable)
                : notificationRepository.findDistinctAdminBroadcasts(adminTypes, pageable);

        return notifications.stream()
                .map(NotificationResponseDto::from)
                .toList();
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

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notiNo) {
        notificationRepository.deleteById(notiNo);
    }
}