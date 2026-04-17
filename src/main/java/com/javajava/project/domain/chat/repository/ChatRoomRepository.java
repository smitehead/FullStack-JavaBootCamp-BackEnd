package com.javajava.project.domain.chat.repository;

import com.javajava.project.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * [동시성 방어] 동일 조건의 ACTIVE 채팅방 존재 여부 확인
     * DB 유니크 인덱스(IDX_CHATROOM_UNIQUE_ACTIVE)와 함께 이중 방어
     */
    Optional<ChatRoom> findByBuyerNoAndSellerNoAndProductNoAndStatus(
            Long buyerNo, Long sellerNo, Long productNo, String status);

    /**
     * [채팅방 목록] ROW_NUMBER() 윈도우 함수로 N+1 문제 해결
     * 한 쿼리로 채팅방 + 최근 메시지 1건 + 안 읽은 수를 모두 조회
     *
     * Oracle 네이티브 쿼리:
     * - ranked_msg: 채팅방별 최신 메시지 1건만 추출 (ROW_NUMBER)
     * - unread_cnt: 상대가 보낸 안 읽은 메시지 수 (서브쿼리)
     * - PRODUCT, MEMBER JOIN: 상품명/이미지, 상대방 닉네임/프로필
     */
    @Query(nativeQuery = true, value = """
        SELECT
            cr.ROOM_NO          AS roomNo,
            cr.BUYER_NO         AS buyerNo,
            cr.SELLER_NO        AS sellerNo,
            cr.PRODUCT_NO       AS productNo,
            p.TITLE             AS productTitle,
            (SELECT '/api/images/' || pi.UUID_NAME
             FROM PRODUCT_IMAGE pi
             WHERE pi.PRODUCT_NO = cr.PRODUCT_NO AND pi.IS_MAIN = 1
             AND ROWNUM = 1)    AS productImage,
            rm.CONTENT          AS lastMessage,
            rm.SENT_AT          AS lastMessageAt,
            rm.SENDER_NO        AS lastSenderNo,
            NVL(uc.cnt, 0)      AS unreadCount,
            CASE WHEN cr.BUYER_NO = :myNo THEN cr.SELLER_NO ELSE cr.BUYER_NO END AS otherUserNo,
            m.NICKNAME          AS otherUserNickname,
            m.PROFILE_IMG_URL   AS otherUserProfileImage
        FROM CHAT_ROOM cr
        LEFT JOIN (
            SELECT cm2.ROOM_NO, cm2.CONTENT, cm2.SENT_AT, cm2.SENDER_NO
            FROM (
                SELECT cm.ROOM_NO, cm.CONTENT, cm.SENT_AT, cm.SENDER_NO,
                       ROW_NUMBER() OVER (PARTITION BY cm.ROOM_NO ORDER BY cm.SENT_AT DESC, cm.MSG_NO DESC) rn
                FROM CHAT_MESSAGE cm
            ) cm2
            WHERE cm2.rn = 1
        ) rm ON cr.ROOM_NO = rm.ROOM_NO
        LEFT JOIN (
            SELECT ROOM_NO, COUNT(*) cnt
            FROM CHAT_MESSAGE
            WHERE SENDER_NO != :myNo AND IS_READ = 0
            GROUP BY ROOM_NO
        ) uc ON cr.ROOM_NO = uc.ROOM_NO
        LEFT JOIN PRODUCT p ON cr.PRODUCT_NO = p.PRODUCT_NO
        LEFT JOIN MEMBER m ON m.MEMBER_NO =
            CASE WHEN cr.BUYER_NO = :myNo THEN cr.SELLER_NO ELSE cr.BUYER_NO END
        WHERE (cr.BUYER_NO = :myNo OR cr.SELLER_NO = :myNo)
          AND cr.STATUS = 'ACTIVE'
          AND NOT (cr.BUYER_NO = :myNo AND cr.BUYER_LEFT = 1)
          AND NOT (cr.SELLER_NO = :myNo AND cr.SELLER_LEFT = 1)
        ORDER BY rm.SENT_AT DESC NULLS LAST
        """)
    List<Object[]> findChatRoomListWithLatestMessage(@Param("myNo") Long myNo);

    /**
     * 특정 사용자가 참여한 채팅방인지 검증 (인가 처리용)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.roomNo = :roomNo AND (cr.buyerNo = :memberNo OR cr.sellerNo = :memberNo)")
    Optional<ChatRoom> findByRoomNoAndParticipant(@Param("roomNo") Long roomNo, @Param("memberNo") Long memberNo);
}