package com.javajava.project.repository;

import com.javajava.project.entity.BidHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {
    
    // 특정 상품의 모든 입찰 내역 조회 (최신순)
    List<BidHistory> findByProductNoOrderByBidTimeDesc(Long productNo);
    
    // 특정 상품의 현재 최고 입찰가 조회
    Optional<BidHistory> findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(Long productNo, Integer isCancelled);
    
    // 회원이 입찰한 내역 조회
    List<BidHistory> findByMemberNo(Long memberNo);
}