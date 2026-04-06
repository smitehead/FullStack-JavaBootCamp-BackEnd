package com.javajava.project.domain.community.repository;

import com.javajava.project.domain.community.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 일반 유저용: 삭제되지 않은 공지 목록 (카테고리 필터 + 검색)
    @Query("SELECT n FROM Notice n WHERE n.isDeleted = 0 " +
            "AND (:category IS NULL OR n.category = :category) " +
            "AND (:keyword IS NULL OR n.title LIKE %:keyword% OR n.description LIKE %:keyword%) " +
            "ORDER BY n.isImportant DESC, n.createdAt DESC")
    Page<Notice> findActiveNotices(@Param("category") String category,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    // 일반 유저용: 삭제되지 않은 단건 조회
    Optional<Notice> findByNoticeNoAndIsDeleted(Long noticeNo, Integer isDeleted);

    // 관리자용: 전체 목록 (삭제 포함)
    List<Notice> findAllByOrderByCreatedAtDesc();
}
