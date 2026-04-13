package com.javajava.project.domain.chat.repository;

import com.javajava.project.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * [커서 페이징] 메시지 내역 조회 (무한 스크롤)
     * msgNo < lastMsgNo 기준으로 최신순 size건 조회
     * 복합 정렬: (SENT_AT DESC, MSG_NO DESC) — 밀리초 충돌 방지
     *
     * Oracle 네이티브 쿼리 (FETCH FIRST N ROWS ONLY는 Oracle 12c+)
     */
    @Query(nativeQuery = true, value = """
        SELECT * FROM (
            SELECT MSG_NO, ROOM_NO, SENDER_NO, CONTENT, SENT_AT, IS_READ
            FROM CHAT_MESSAGE
            WHERE ROOM_NO = :roomNo AND MSG_NO < :lastMsgNo
            ORDER BY SENT_AT DESC, MSG_NO DESC
        ) WHERE ROWNUM <= :size
        """)
    List<Object[]> findMessagesBeforeCursor(
            @Param("roomNo") Long roomNo,
            @Param("lastMsgNo") Long lastMsgNo,
            @Param("size") int size);

    /**
     * [첫 페이지] lastMsgNo 없이 최신 메시지부터 조회
     */
    @Query(nativeQuery = true, value = """
        SELECT * FROM (
            SELECT MSG_NO, ROOM_NO, SENDER_NO, CONTENT, SENT_AT, IS_READ
            FROM CHAT_MESSAGE
            WHERE ROOM_NO = :roomNo
            ORDER BY SENT_AT DESC, MSG_NO DESC
        ) WHERE ROWNUM <= :size
        """)
    List<Object[]> findLatestMessages(
            @Param("roomNo") Long roomNo,
            @Param("size") int size);

    /**
     * [재연결 복구] 특정 msgNo 이후의 메시지 조회
     * WebSocket 재연결 시 누락 메시지 보충용
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomNo = :roomNo AND cm.msgNo > :afterMsgNo ORDER BY cm.sentAt ASC, cm.msgNo ASC")
    List<ChatMessage> findMessagesAfter(
            @Param("roomNo") Long roomNo,
            @Param("afterMsgNo") Long afterMsgNo);

    /**
     * [벌크 읽음 처리] 방 입장 시 상대방 메시지 일괄 읽음 처리
     * 나(readerNo)가 아닌 상대방이 보낸 미읽음 메시지만 IS_READ = 1로 업데이트
     */
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = 1 WHERE cm.roomNo = :roomNo AND cm.senderNo != :readerNo AND cm.isRead = 0")
    int bulkMarkAsRead(@Param("roomNo") Long roomNo, @Param("readerNo") Long readerNo);
}