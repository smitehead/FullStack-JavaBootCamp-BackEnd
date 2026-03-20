package com.javajava.project.config;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.sqm.FetchClauseType;

public class Oracle11gDialect extends OracleDialect {

    // Oracle 11g는 OFFSET ... FETCH FIRST 문법 미지원
    // false 반환 시 Hibernate가 ROWNUM 기반 페이징으로 자동 전환
    @Override
    public boolean supportsFetchClause(FetchClauseType type) {
        return false;
    }
}
