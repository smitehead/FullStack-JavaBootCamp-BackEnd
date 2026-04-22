-- ============================================================
-- 1. CHAT_MESSAGE 테이블 수정 (IMAGE_URL 컬럼은 제거)
-- ============================================================
ALTER TABLE CHAT_MESSAGE ADD MSG_TYPE VARCHAR2(20) DEFAULT 'TEXT' NOT NULL;
-- IMAGE_URL은 추가하지 않음 (별도 테이블로 관리)

-- 주소는 1:1 관계이므로 그대로 컬럼 추가
ALTER TABLE CHAT_MESSAGE ADD ADDR_ROAD   VARCHAR2(200);
ALTER TABLE CHAT_MESSAGE ADD ADDR_DETAIL VARCHAR2(255);
ALTER TABLE CHAT_MESSAGE ADD LATITUDE    NUMBER(10,7);
ALTER TABLE CHAT_MESSAGE ADD LONGITUDE   NUMBER(10,7);

-- ============================================================
-- 2. CHAT_IMAGE 테이블 신규 생성 (기존 이미지 테이블 패턴 동일)
-- ============================================================
CREATE TABLE CHAT_IMAGE (
    IMAGE_NO      NUMBER PRIMARY KEY,        -- PK
    MSG_NO        NUMBER NOT NULL,           -- CHAT_MESSAGE.MSG_NO FK
    ORIGINAL_NAME VARCHAR2(255) NOT NULL,    -- 원본 파일명
    UUID_NAME     VARCHAR2(255) NOT NULL,    -- 서버 저장 파일명
    IMAGE_PATH    VARCHAR2(500) NOT NULL,    -- 물리 경로
    SORT_ORDER    NUMBER DEFAULT 0 NOT NULL, -- 이미지 순서 (0, 1, 2, ...)
    CREATED_AT    DATE DEFAULT SYSDATE NOT NULL
);

CREATE SEQUENCE CHAT_IMAGE_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

-- 메시지별 이미지 조회 + 순서 정렬용 인덱스
CREATE INDEX IDX_CHAT_IMAGE_MSG ON CHAT_IMAGE (MSG_NO, SORT_ORDER);