-- =============================================
-- Review 테이블 수정: 별점 선택적 + 태그 추가
-- =============================================

-- 1. RATING 컬럼 NOT NULL → NULL 허용 (ddl-auto=update가 못 하는 부분)
ALTER TABLE REVIEW MODIFY (RATING NUMBER NULL);

-- 2. TAGS 컬럼 추가 (콤마 구분 문자열, 예: '응답이 빨라요,친절하고 매너가 좋아요')
ALTER TABLE REVIEW ADD (TAGS VARCHAR2(500));

-- =============================================
-- Notice 테이블 수정: 카테고리/설명/중요공지 추가
-- =============================================

-- 3. CATEGORY 컬럼 추가 (업데이트/이벤트/점검/정책)
ALTER TABLE NOTICE ADD (CATEGORY VARCHAR2(20) DEFAULT '업데이트' NOT NULL);

-- 4. DESCRIPTION 컬럼 추가 (짧은 설명)
ALTER TABLE NOTICE ADD (DESCRIPTION VARCHAR2(300));

-- 5. IS_IMPORTANT 컬럼 추가 (1이면 중요 공지)
ALTER TABLE NOTICE ADD (IS_IMPORTANT NUMBER(1) DEFAULT 0 NOT NULL);
