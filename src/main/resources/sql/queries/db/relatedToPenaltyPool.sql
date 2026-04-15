-- ============================================================
-- 2026-04-15 추가 마이그레이션
-- 입찰 취소 로직 수정 (5% 위약금 풀 / 강제 승계 플래그)
-- ============================================================

-- 1. PRODUCT: 위약금 누적 풀
ALTER TABLE PRODUCT
    ADD PENALTY_POOL NUMBER DEFAULT 0 NOT NULL;

COMMENT ON COLUMN PRODUCT.PENALTY_POOL
    IS '입찰 취소 위약금(5%) 누적 풀. 결제 완료 시 2.5% 판매자/2.5% 낙찰자 분배. 최종 유찰(CLOSED_FAILED) 시 전액 판매자 지급 후 0으로 초기화.';

-- 2. AUCTION_RESULT: 강제 승계 여부 (매너 패널티 면제 구분자)
ALTER TABLE AUCTION_RESULT
    ADD IS_FORCE_PROMOTED NUMBER(1) DEFAULT 0 NOT NULL;

COMMENT ON COLUMN AUCTION_RESULT.IS_FORCE_PROMOTED
    IS '0: 자발적 낙찰(정상). 1: 스케줄러 강제 승계. 1인 경우 결제 불이행 시 매너온도 패널티 없음.';

-- ============================================================
-- 적용 확인
-- ============================================================
SELECT COLUMN_NAME, DATA_TYPE, DATA_DEFAULT, NULLABLE
FROM   USER_TAB_COLUMNS
WHERE  TABLE_NAME = 'PRODUCT'     AND COLUMN_NAME = 'PENALTY_POOL'
UNION ALL
SELECT COLUMN_NAME, DATA_TYPE, DATA_DEFAULT, NULLABLE
FROM   USER_TAB_COLUMNS
WHERE  TABLE_NAME = 'AUCTION_RESULT' AND COLUMN_NAME = 'IS_FORCE_PROMOTED';