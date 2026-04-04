package com.javajava.project.domain.community.repository;

import com.javajava.project.domain.community.entity.Qna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    // 특정 상품의 Q&A 목록 조회
    List<Qna> findByProductNo(Long productNo);

    // 특정 입찰자가 남긴 Q&A 조회
    List<Qna> findByBidNo(Long bidNo);

    // 특정 상품의 Q&A 수 조회
    long countByProductNo(Long productNo);
}