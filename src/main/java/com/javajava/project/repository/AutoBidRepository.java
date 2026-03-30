package com.javajava.project.repository;

import com.javajava.project.entity.AutoBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutoBidRepository extends JpaRepository<AutoBid, Long> {

    // 특정 상품의 활성 자동입찰 목록 (최대가 높은 순)
    @Query("SELECT a FROM AutoBid a WHERE a.productNo = :productNo AND a.isActive = 1 ORDER BY a.maxPrice DESC")
    List<AutoBid> findActiveByProductNo(@Param("productNo") Long productNo);

    // 특정 회원의 특정 상품 활성 자동입찰 조회
    Optional<AutoBid> findByMemberNoAndProductNoAndIsActive(Long memberNo, Long productNo, Integer isActive);

    // 특정 회원의 특정 상품 자동입찰 여부 확인
    boolean existsByMemberNoAndProductNoAndIsActive(Long memberNo, Long productNo, Integer isActive);
}
