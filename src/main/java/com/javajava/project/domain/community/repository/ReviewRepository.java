package com.javajava.project.domain.community.repository;

import com.javajava.project.domain.community.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 회원이 받은 모든 리뷰 조회 (최신순)
    List<Review> findByTargetNoOrderByCreatedAtDesc(Long targetNo);

    // 특정 회원이 작성한 리뷰 목록 조회
    List<Review> findByWriterNo(Long writerNo);

    // 특정 낙찰 결과에 대한 리뷰 조회 (거래 1건당 리뷰 1개 원칙)
    Optional<Review> findByResultNo(Long resultNo);

    // 회원의 공개 리뷰만 조회
    List<Review> findByTargetNoAndIsHidden(Long targetNo, Integer isHidden);

    // 특정 회원이 받은 공개 리뷰 평균 별점
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.targetNo = :targetNo AND r.isHidden = 0")
    Double findAverageRatingByTargetNo(@Param("targetNo") Long targetNo);
}