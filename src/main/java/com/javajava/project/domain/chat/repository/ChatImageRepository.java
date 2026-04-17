package com.javajava.project.domain.chat.repository;

import com.javajava.project.domain.chat.entity.ChatImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatImageRepository extends JpaRepository<ChatImage, Long> {

    /**
     * 단일 메시지의 이미지 조회 (순서대로)
     */
    List<ChatImage> findByMsgNoOrderBySortOrderAsc(Long msgNo);

    /**
     * [N+1 방지] 여러 메시지의 이미지를 IN 쿼리로 한 번에 조회
     * 메시지 목록 조회 시 쿼리 수를 2개로 고정 (메시지 조회 1 + 이미지 일괄 조회 1)
     */
    @Query("SELECT ci FROM ChatImage ci WHERE ci.msgNo IN :msgNos ORDER BY ci.msgNo, ci.sortOrder ASC")
    List<ChatImage> findByMsgNoIn(@Param("msgNos") List<Long> msgNos);
}
