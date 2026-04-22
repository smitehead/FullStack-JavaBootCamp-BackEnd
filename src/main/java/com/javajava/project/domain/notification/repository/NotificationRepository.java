package com.javajava.project.domain.notification.repository;

import com.javajava.project.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
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

    // 관리자 발송 알림 (중복 제거) — 전체 타입
    @Query(value = "SELECT n.NOTI_NO, n.MEMBER_NO, n.TYPE, n.CONTENT, n.LINK_URL, n.IS_READ, n.CREATED_AT " +
                   "FROM NOTIFICATION n " +
                   "WHERE n.NOTI_NO IN (" +
                   "  SELECT MIN(n2.NOTI_NO) FROM NOTIFICATION n2 WHERE n2.TYPE IN (:types) " +
                   "  GROUP BY n2.TYPE, n2.CONTENT, n2.LINK_URL" +
                   ") ORDER BY n.CREATED_AT DESC",
           nativeQuery = true)
    List<Notification> findDistinctAdminBroadcasts(@Param("types") List<String> types, Pageable pageable);

    // 관리자 발송 알림 (중복 제거) — 특정 타입
    @Query(value = "SELECT n.NOTI_NO, n.MEMBER_NO, n.TYPE, n.CONTENT, n.LINK_URL, n.IS_READ, n.CREATED_AT " +
                   "FROM NOTIFICATION n " +
                   "WHERE n.NOTI_NO IN (" +
                   "  SELECT MIN(n2.NOTI_NO) FROM NOTIFICATION n2 WHERE n2.TYPE = :type " +
                   "  GROUP BY n2.TYPE, n2.CONTENT, n2.LINK_URL" +
                   ") ORDER BY n.CREATED_AT DESC",
           nativeQuery = true)
    List<Notification> findDistinctAdminBroadcastsByType(@Param("type") String type, Pageable pageable);
}