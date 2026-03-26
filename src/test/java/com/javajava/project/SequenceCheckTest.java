package com.javajava.project; // 실제 프로젝트 패키지 경로에 맞게 수정하세요.

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@SpringBootTest
public class SequenceCheckTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void checkDatabaseSequences() {
        System.out.println("======= DB 시퀀스 목록 조회 시작 =======");
        
        // Oracle의 USER_SEQUENCES 뷰에서 시퀀스 이름 추출
        String sql = "SELECT SEQUENCE_NAME FROM USER_SEQUENCES ORDER BY SEQUENCE_NAME";
        
        List<String> sequences = jdbcTemplate.queryForList(sql, String.class);
        
        if (sequences.isEmpty()) {
            System.out.println("조회된 시퀀스가 없습니다.");
        } else {
            sequences.forEach(name -> System.out.println("Found Sequence: " + name));
        }
        
        System.out.println("======= DB 시퀀스 목록 조회 종료 =======");
    }
}