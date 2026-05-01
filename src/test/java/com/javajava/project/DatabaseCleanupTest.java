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
    @DisplayName("DB 전체 초기화 (모든 테이블 삭제 + 시퀀스 리셋)")
    @Rollback(false) // 테스트가 끝나도 롤백하지 않고 실제 DB에 삭제 반영
    void clearDatabaseData() {
        System.out.println("======= DB 데이터 초기화 시작 =======");

        // -----------------------------------------------
        // STEP 1. 데이터 삭제 (자식 테이블부터 부모 순으로)
        // -----------------------------------------------
        String[] deleteSqls = {
            // ── 최하위 단말 자식 테이블 ──
            "DELETE FROM PRODUCT_IMAGE",    // (FK → PRODUCT)
            "DELETE FROM INQUIRY_IMAGE",    // (FK → INQUIRY)
            "DELETE FROM REPORT_IMAGE",     // (FK → REPORT)
            "DELETE FROM CHAT_IMAGE",       // (FK → CHAT_MESSAGE)
            "DELETE FROM REVIEW_TAG",       // (FK → REVIEW)
            "DELETE FROM ACTIVITY_LOG",     // (FK → MEMBER)
            "DELETE FROM NOTICE",           // (FK → MEMBER)
            "DELETE FROM NOTIFICATION",     // (FK → MEMBER)
            "DELETE FROM MANNER_HISTORY",   // (FK → MEMBER)
            "DELETE FROM BLOCKED_USER",     // (FK → MEMBER)
            "DELETE FROM BANK_ACCOUNT",     // (FK → MEMBER)
            "DELETE FROM POINT_HISTORY",    // (FK → MEMBER)
            "DELETE FROM POINT_WITHDRAW",   // (FK → MEMBER)
            "DELETE FROM POINT_CHARGE",     // (FK → MEMBER)
            "DELETE FROM BILLING_KEY",      // (FK → MEMBER)
            "DELETE FROM WISHLIST",         // (FK → PRODUCT, MEMBER)
            "DELETE FROM AUTO_BID",         // (FK → PRODUCT, MEMBER)
            // ── AUCTION_RESULT를 참조하는 테이블 먼저 ──
            "DELETE FROM REVIEW",           // (FK → AUCTION_RESULT, MEMBER)
            // ── CHAT_ROOM을 참조하는 테이블 먼저 ──
            "DELETE FROM CHAT_MESSAGE",     // (FK → CHAT_ROOM, MEMBER)
            // ── BID_HISTORY를 참조하는 테이블 먼저 ──
            "DELETE FROM QNA",              // (FK → PRODUCT, MEMBER, BID_HISTORY)
            "DELETE FROM AUCTION_RESULT",   // (FK → BID_HISTORY)
            // ── PRODUCT를 참조하는 테이블 먼저 ──
            "DELETE FROM CHAT_ROOM",        // (FK → PRODUCT, MEMBER)
            "DELETE FROM BID_HISTORY",      // (FK → PRODUCT, MEMBER)
            "DELETE FROM PLATFORM_REVENUE", // (FK → PRODUCT, MEMBER)
            "DELETE FROM REPORT",           // (FK → PRODUCT, MEMBER)
            "DELETE FROM INQUIRY",          // (FK → MEMBER)
            "DELETE FROM PRODUCT",          // (FK → MEMBER, CATEGORY)
            "DELETE FROM MEMBER"            // 최상위 부모
        };

        for (String sql : deleteSqls) {
            try {
                int deletedCount = jdbcTemplate.update(sql);
                System.out.println("성공: " + sql + " (삭제된 행 수: " + deletedCount + ")");
            } catch (Exception e) {
                System.out.println("알림: " + sql + " 실행 중 오류 또는 건너뜀 (" + e.getMessage().split("\n")[0] + ")");
            }
        }

        // -----------------------------------------------
        // STEP 2. 시퀀스 리셋 (DROP → CREATE로 1부터 재시작)
        // ※ CATEGORY, REVIEW_TAG_DEF 는 기준 데이터이므로 제외
        // -----------------------------------------------
        String[][] sequences = {
            { "MEMBER_SEQ",           "회원" },
            { "PRODUCT_SEQ",          "상품" },
            { "BID_SEQ",              "입찰" },
            { "RESULT_SEQ",           "경매 결과" },
            { "PRODUCT_IMAGE_SEQ",    "상품 이미지" },
            { "INQUIRY_IMAGE_SEQ",    "문의 이미지" },
            { "REPORT_IMAGE_SEQ",     "신고 이미지" },
            { "CHAT_ROOM_SEQ",        "채팅방" },
            { "CHAT_MSG_SEQ",         "채팅 메시지" },
            { "CHAT_IMAGE_SEQ",       "채팅 이미지" },
            { "AUTO_BID_SEQ",         "자동입찰" },
            { "NOTIFICATION_SEQ",     "알림" },
            { "NOTICE_SEQ",           "공지사항" },
            { "ACTIVITY_LOG_SEQ",     "관리자 활동 로그" },
            { "MANNER_HISTORY_SEQ",   "매너온도 내역" },
            { "BANK_ACCOUNT_SEQ",     "은행 계좌" },
            { "PLATFORM_REVENUE_SEQ", "플랫폼 수익" },
            { "POINT_HISTORY_SEQ",    "포인트 내역" },
            { "POINT_WITHDRAW_SEQ",   "포인트 출금" },
            { "POINT_CHARGE_SEQ",     "포인트 충전" },
            { "BILLING_KEY_SEQ",      "빌링키" },
            { "REVIEW_SEQ",           "리뷰" },
            { "WISHLIST_SEQ",         "찜" },
            { "QNA_SEQ",              "QNA" },
            { "INQUIRY_SEQ",          "고객 문의" },
            { "REPORT_SEQ",           "신고" }
        };

        System.out.println("======= 시퀀스 리셋 시작 =======");

        for (String[] seq : sequences) {
            String seqName    = seq[0];
            String seqComment = seq[1];
            try {
                jdbcTemplate.execute("DROP SEQUENCE " + seqName);
                jdbcTemplate.execute(
                    "CREATE SEQUENCE " + seqName +
                    " START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE"
                );
                System.out.println("성공: " + seqName + " 리셋 완료 (" + seqComment + ")");
            } catch (Exception e) {
                System.out.println("알림: " + seqName + " 리셋 중 오류 또는 건너뜀 (" + e.getMessage().split("\n")[0] + ")");
            }
        }

        System.out.println("======= DB 데이터 초기화 종료 =======");
    }
}