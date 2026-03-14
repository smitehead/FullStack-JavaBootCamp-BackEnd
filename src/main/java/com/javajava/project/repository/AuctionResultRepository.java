package com.javajava.project.repository;

import com.javajava.project.entity.AuctionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {
    
    // 입찰 번호로 낙찰 결과 조회
    Optional<AuctionResult> findByBidNo(Long bidNo);
    
    // 현재 거래 상태별 목록 조회 (예: 배송대기 중인 건들)
    List<AuctionResult> findByStatus(String status);
}