-- ============================================================
-- 경매 최고 입찰자 취소 & 차순위 승계 기능 DB 마이그레이션
-- 대상 DB : Oracle
-- 적용 순서 : 이 파일을 위에서 아래로 순서대로 실행
-- ============================================================


-- ============================================================
-- SECTION 1. PRODUCT 테이블 변경
-- ============================================================

-- 1-1. 재등록 이력 추적용 자기참조 컬럼 추가
ALTER TABLE PRODUCT
    ADD PARENT_PRODUCT_NO NUMBER;

COMMENT ON COLUMN PRODUCT.PARENT_PRODUCT_NO
    IS '재등록 시 원본(유찰된) 상품번호. 유찰 파생 이력 및 통계 추적용';

-- 1-2. 자기참조 FK (데이터 무결성)
ALTER TABLE PRODUCT
    ADD CONSTRAINT FK_PRODUCT_PARENT
        FOREIGN KEY (PARENT_PRODUCT_NO) REFERENCES PRODUCT (PRODUCT_NO);

-- 1-3. STATUS 코멘트 갱신
--   기존: 0=진행중, 1=낙찰완료, 2=취소/유찰
--   변경: 2=판매자취소, 4=유찰/최종실패 분리, 3=결제대기 신규 추가
COMMENT ON COLUMN PRODUCT.STATUS
    IS '0: 진행중(ACTIVE) | 1: 낙찰완료(COMPLETED) | 2: 판매자취소(CANCELED) | 3: 결제대기(PENDING_PAYMENT) | 4: 유찰/최종실패(CLOSED_FAILED)';

-- 1-4. 조회 성능 인덱스
-- Phase2 배치 스케줄러:
--   WHERE STATUS = 3 AND END_TIME < SYSDATE 패턴에 최적화
CREATE INDEX IDX_PRODUCT_STATUS_ENDTIME
    ON PRODUCT (STATUS, END_TIME);

-- 재등록 상품 원본 추적 조회용
CREATE INDEX IDX_PRODUCT_PARENT
    ON PRODUCT (PARENT_PRODUCT_NO);


-- ============================================================
-- SECTION 2. BID_HISTORY 테이블 변경
-- ============================================================

-- 2-1. 입찰 취소 조회 성능 인덱스
-- BidCancelService / AuctionPaymentScheduler:
--   WHERE PRODUCT_NO = ? AND IS_CANCELLED = 0 ORDER BY BID_PRICE DESC 패턴
CREATE INDEX IDX_BID_PRODUCT_CANCEL_PRICE
    ON BID_HISTORY (PRODUCT_NO, IS_CANCELLED, BID_PRICE DESC);

-- 특정 회원의 특정 상품 입찰 기록 빠른 조회 (취소 대상 입찰 검증용)
CREATE INDEX IDX_BID_MEMBER_PRODUCT
    ON BID_HISTORY (MEMBER_NO, PRODUCT_NO, IS_CANCELLED);


-- ============================================================
-- SECTION 3. AUCTION_RESULT 테이블 변경
-- ============================================================

-- 3-1. 결제 마감 일시 컬럼 추가 (차순위 승계 시 12시간씩 갱신)
ALTER TABLE AUCTION_RESULT
    ADD PAYMENT_DUE_DATE DATE;

COMMENT ON COLUMN AUCTION_RESULT.PAYMENT_DUE_DATE
    IS '결제 마감 일시. 낙찰 확정 시 +12시간 세팅, 차순위 승계 시 다시 +12시간 갱신됨';

-- 3-2. STATUS 코멘트 갱신
COMMENT ON COLUMN AUCTION_RESULT.STATUS
    IS '거래 진행상태. 배송대기(결제전) → 결제완료 → 배송중 → 구매확정 / 거래취소 / 결제기한만료';

-- 3-3. 조회 성능 인덱스
-- Phase2 배치 스케줄러:
--   WHERE STATUS = '배송대기' AND PAYMENT_DUE_DATE < SYSDATE 패턴
CREATE INDEX IDX_AUCTIONRESULT_STATUS_DUE
    ON AUCTION_RESULT (STATUS, PAYMENT_DUE_DATE);


-- ============================================================
-- SECTION 4. POINT_HISTORY 타입 코멘트 갱신
-- ============================================================
COMMENT ON COLUMN POINT_HISTORY.TYPE
    IS '변동 유형. 충전 | 낙찰차감 | 판매정산 | 출금 | 관리자조정 | 거래취소환불 | 거래취소회수 | 위약금차감 | 취소보상금';


-- ============================================================
-- SECTION 5. 적용 확인 쿼리 (실행 후 결과 검증용)
-- ============================================================

-- PRODUCT 신규 컬럼 확인
SELECT COLUMN_NAME, DATA_TYPE, NULLABLE
FROM   USER_TAB_COLUMNS
WHERE  TABLE_NAME = 'PRODUCT'
AND    COLUMN_NAME IN ('PARENT_PRODUCT_NO', 'STATUS');

-- AUCTION_RESULT 신규 컬럼 확인
SELECT COLUMN_NAME, DATA_TYPE, NULLABLE
FROM   USER_TAB_COLUMNS
WHERE  TABLE_NAME = 'AUCTION_RESULT'
AND    COLUMN_NAME IN ('PAYMENT_DUE_DATE', 'STATUS');

-- 신규 인덱스 확인
SELECT INDEX_NAME, TABLE_NAME, STATUS
FROM   USER_INDEXES
WHERE  INDEX_NAME IN (
    'IDX_PRODUCT_STATUS_ENDTIME',
    'IDX_PRODUCT_PARENT',
    'IDX_BID_PRODUCT_CANCEL_PRICE',
    'IDX_BID_MEMBER_PRODUCT',
    'IDX_AUCTIONRESULT_STATUS_DUE'
);

-- FK 확인
SELECT CONSTRAINT_NAME, STATUS
FROM   USER_CONSTRAINTS
WHERE  CONSTRAINT_NAME = 'FK_PRODUCT_PARENT';
