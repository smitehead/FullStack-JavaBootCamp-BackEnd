package com.javajava.project;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

@Disabled("필요할 때만 수동으로 주석 해제하고 실행하기 위한 DB 유틸리티")
@SpringBootTest
public class DatabaseCleanupTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("DB 데이터 초기화 (PRODUCT 및 연관 테이블 삭제)")
    @Rollback(false) // 테스트가 끝나도 롤백하지 않고 실제 DB에 삭제 반영
    void clearDatabaseData() {
        System.out.println("======= DB 데이터 초기화 시작 =======");

        // 자식 테이블부터 부모 테이블 순으로 삭제 (외래 키 제약 조건 방지)
        String[] deleteSqls = {
            "DELETE FROM PRODUCT_IMAGE", // 상품 이미지
            "DELETE FROM BID_HISTORY",   // 입찰 기록
            "DELETE FROM WISHLIST",      // 찜(관심상품)
            "DELETE FROM QNA",           // 상품 문의
            "DELETE FROM REVIEW",        // 리뷰
            "DELETE FROM PRODUCT"        // 마지막으로 상품 테이블 삭제
        };

        for (String sql : deleteSqls) {
            try {
                int deletedCount = jdbcTemplate.update(sql);
                System.out.println("성공: " + sql + " (삭제된 행 수: " + deletedCount + ")");
            } catch (Exception e) {
                // 테이블이 없거나 다른 참조 에러가 날 경우 로그만 출력하고 계속 진행
                System.out.println("알림: " + sql + " 실행 중 오류 또는 건너뜀 (" + e.getMessage().split("\n")[0] + ")");
            }
        }

        System.out.println("======= DB 데이터 초기화 종료 =======");
    }
}