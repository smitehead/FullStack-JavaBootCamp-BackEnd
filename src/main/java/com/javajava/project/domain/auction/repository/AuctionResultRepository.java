package com.javajava.project.domain.auction.repository;

import com.javajava.project.domain.auction.entity.AuctionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {

    // 입찰 번호로 낙찰 결과 조회
    // findFirst: 중복 처리로 인해 레코드가 2개 있어도 NonUniqueResultException 방지
    Optional<AuctionResult> findFirstByBidNo(Long bidNo);

    // 현재 거래 상태별 목록 조회 (예: 배송대기 중인 건들)
    List<AuctionResult> findByStatus(String status);

    // 여러 입찰 번호로 낙찰 결과 배치 조회
    @Query("SELECT ar FROM AuctionResult ar WHERE ar.bidNo IN :bidNos")
    List<AuctionResult> findByBidNos(@Param("bidNos") List<Long> bidNos);

    /**
     * Phase2 AuctionPaymentScheduler 전용.
     * 결제 마감 시간이 지났고 아직 '배송대기' 상태인 AuctionResult 조회.
     * Oracle NATIVE QUERY 사용 (JPA가 관계 미매핑 엔티티 조인을 지원하지 않으므로).
     *
     * BID_HISTORY와 JOIN해서 PRODUCT_NO까지 한 번에 가져옴 → N+1 방지.
     */
    @Query(value = """
        SELECT ar.*
        FROM   AUCTION_RESULT ar
        JOIN   BID_HISTORY    bh ON ar.BID_NO = bh.BID_NO
        WHERE  ar.STATUS            = '배송대기'
        AND    ar.PAYMENT_DUE_DATE  < :now
        """, nativeQuery = true)
    List<AuctionResult> findExpiredPendingPayments(@Param("now") LocalDateTime now);
}