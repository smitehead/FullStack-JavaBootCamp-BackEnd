package com.javajava.project.repository;

import com.javajava.project.entity.AuctionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {

    // 입찰 번호로 낙찰 결과 조회
    Optional<AuctionResult> findByBidNo(Long bidNo);

    // 현재 거래 상태별 목록 조회 (예: 배송대기 중인 건들)
    List<AuctionResult> findByStatus(String status);

    // 여러 입찰 번호로 낙찰 결과 배치 조회
    @Query("SELECT ar FROM AuctionResult ar WHERE ar.bidNo IN :bidNos")
    List<AuctionResult> findByBidNos(@Param("bidNos") List<Long> bidNos);
}