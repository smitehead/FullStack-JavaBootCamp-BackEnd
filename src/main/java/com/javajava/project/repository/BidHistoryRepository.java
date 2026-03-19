package com.javajava.project.repository;

import com.javajava.project.entity.BidHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {
    
    // 특정 상품의 모든 입찰 내역 조회 (최신순)
    List<BidHistory> findByProductNoOrderByBidTimeDesc(Long productNo);
    
    // 특정 상품의 현재 최고 입찰가 조회
    Optional<BidHistory> findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(Long productNo, Integer isCancelled);
    
    // 회원이 입찰한 내역 조회
    List<BidHistory> findByMemberNo(Long memberNo);

    //입찰 내역(b)과 회원(m)의 닉네임을 조인하여 한 번에 가져옵니다.
    @Query("SELECT b, m.nickname FROM BidHistory b " +
           "JOIN Member m ON b.memberNo = m.memberNo " +
           "WHERE b.productNo = :productNo " +
           "ORDER BY b.bidTime DESC")
    List<Object[]> findBidHistoryWithNickname(@Param("productNo") Long productNo);
}