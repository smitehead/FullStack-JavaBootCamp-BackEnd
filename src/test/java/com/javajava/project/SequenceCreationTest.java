package com.javajava.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

@SpringBootTest
public class SequenceCreationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Rollback(false) // DDL은 트랜잭션 롤백이 안 되지만, 명시적으로 설정
    void createNewSequences() {
        System.out.println("======= 신규 시퀀스 생성 시작 =======");

        String[] sqls = {
            "CREATE SEQUENCE ACTIVITY_LOG_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE",
            "CREATE SEQUENCE MANNER_HISTORY_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE",
            "CREATE SEQUENCE QNA_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE"
        };

        for (String sql : sqls) {
            try {
                jdbcTemplate.execute(sql);
                System.out.println("성공: " + sql.split(" ")[2]);
            } catch (Exception e) {
                // 이미 존재할 경우 ORA-00955 에러 발생
                System.out.println("알림: " + sql.split(" ")[2] + "는 이미 존재하거나 생성할 수 없습니다. (" + e.getMessage() + ")");
            }
        }

        System.out.println("======= 신규 시퀀스 생성 종료 =======");
    }
}