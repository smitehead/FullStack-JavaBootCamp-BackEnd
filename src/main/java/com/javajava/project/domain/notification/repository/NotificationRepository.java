package com.javajava.project.domain.notification.repository;

import com.javajava.project.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 특정 회원의 알림을 최신순으로 조회
    List<Notification> findByMemberNoOrderByCreatedAtDesc(Long memberNo);
    
    // 특정 회원의 읽지 않은 알림(isRead = 0) 개수 조회 (헤더 알림 뱃지용)
    long countByMemberNoAndIsRead(Long memberNo, Integer isRead);

    // 특정 회원의 읽지 않은 알림 전체를 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = 1 WHERE n.memberNo = :memberNo AND n.isRead = 0")
    int markAllAsRead(@Param("memberNo") Long memberNo);

    // 특정 기준일 이전의 알림 내역을 성능 저하 없이 일괄 삭제
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}