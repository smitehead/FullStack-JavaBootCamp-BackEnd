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
     * 7일 자동 구매 확정 대상 조회 (AuctionAutoConfirmScheduler 전용).
     * paymentDueDate 가 현재 시각 이전인 '배송대기' 건 — 낙찰일로부터 7일 경과.
     * (기존 AuctionPaymentScheduler도 이 쿼리를 공유했으나 해당 스케줄러는 비활성화됨)
     */
    @Query(value = """
        SELECT ar.*
        FROM   AUCTION_RESULT ar
        JOIN   BID_HISTORY    bh ON ar.BID_NO = bh.BID_NO
        WHERE  ar.STATUS            = '배송대기'
        AND    ar.PAYMENT_DUE_DATE  < :now
        """, nativeQuery = true)
    List<AuctionResult> findExpiredPendingPayments(@Param("now") LocalDateTime now);

    /**
     * 구매 확정 D-1/D-2 리마인더 대상 조회 (AuctionAutoConfirmScheduler 전용).
     * paymentDueDate 가 [from, to) 구간에 속하는 '배송대기' 건.
     *
     * <ul>
     *   <li>D-2 알림: from=now+24h, to=now+48h (자동 확정 2일 전 구간)</li>
     *   <li>D-1 알림: from=now,     to=now+24h (자동 확정 1일 전 구간)</li>
     * </ul>
     */
    @Query(value = """
        SELECT ar.*
        FROM   AUCTION_RESULT ar
        WHERE  ar.STATUS           = '배송대기'
        AND    ar.PAYMENT_DUE_DATE >  :from
        AND    ar.PAYMENT_DUE_DATE <= :to
        """, nativeQuery = true)
    List<AuctionResult> findPendingInWindow(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);
}