-- ============================================================
-- 1. 테이블 컬럼 수정
-- ============================================================

-- CONTENT 길이 확장 (1000 → 4000)
ALTER TABLE CHAT_MESSAGE MODIFY CONTENT VARCHAR2(4000);

-- STATUS 기본값을 영문으로 변경 (한글 '활성' → 영문 'ACTIVE')
-- 기존 데이터가 있다면 마이그레이션 후 진행
ALTER TABLE CHAT_ROOM MODIFY STATUS DEFAULT 'ACTIVE';
-- UPDATE CHAT_ROOM SET STATUS = 'ACTIVE' WHERE STATUS = '활성';
-- UPDATE CHAT_ROOM SET STATUS = 'DELETED' WHERE STATUS = '종료';

-- ============================================================
-- 2. 시퀀스 확인 (이미 존재하면 생략)
-- ============================================================
SELECT SEQUENCE_NAME 
FROM USER_SEQUENCES 
WHERE SEQUENCE_NAME IN ('CHAT_ROOM_SEQ', 'CHAT_MESSAGE_SEQ');
-- CREATE SEQUENCE CHAT_ROOM_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;
-- CREATE SEQUENCE CHAT_MESSAGE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

-- ============================================================
-- 3. 성능 최적화 인덱스
-- ============================================================

-- [채팅방] 중복 방지용 함수 기반 유니크 인덱스 (Oracle은 WHERE 절 부분 인덱스 미지원)
SELECT INDEX_NAME FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_CHATROOM_UNIQUE_ACTIVE'

CREATE UNIQUE INDEX IDX_CHATROOM_UNIQUE_ACTIVE ON CHAT_ROOM (CASE WHEN STATUS='ACTIVE' THEN BUYER_NO END, CASE WHEN STATUS='ACTIVE' THEN SELLER_NO END, CASE WHEN STATUS='ACTIVE' THEN PRODUCT_NO END)

-- [채팅방] 내 방 목록 조회용
CREATE INDEX IDX_CHATROOM_BUYER  ON CHAT_ROOM (BUYER_NO, STATUS);
CREATE INDEX IDX_CHATROOM_SELLER ON CHAT_ROOM (SELLER_NO, STATUS);

-- [메시지] 커서 페이징 + 최신 메시지 정렬용
CREATE INDEX IDX_CHATMSG_ROOM_ORDER ON CHAT_MESSAGE (ROOM_NO, SENT_AT DESC, MSG_NO DESC);

-- [메시지] 안 읽은 메시지 카운트용
CREATE INDEX IDX_CHATMSG_UNREAD ON CHAT_MESSAGE (ROOM_NO, SENDER_NO, IS_READ);