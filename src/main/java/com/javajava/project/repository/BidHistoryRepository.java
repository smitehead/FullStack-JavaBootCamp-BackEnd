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

    // 특정 상품에 입찰한 고유 회원 수 조회
    @Query("SELECT COUNT(DISTINCT b.memberNo) FROM BidHistory b WHERE b.productNo = :productNo AND b.isCancelled = 0")
    Long countDistinctParticipants(@Param("productNo") Long productNo);

    // 여러 상품의 참여자 수를 한 번에 조회 (N+1 방지)
    @Query("SELECT b.productNo, COUNT(DISTINCT b.memberNo) FROM BidHistory b WHERE b.productNo IN :productNos AND b.isCancelled = 0 GROUP BY b.productNo")
    List<Object[]> countDistinctParticipantsByProductNos(@Param("productNos") List<Long> productNos);

    // 마이페이지: 특정 회원이 입찰한 고유 상품 번호 목록
    @Query("SELECT DISTINCT b.productNo FROM BidHistory b WHERE b.memberNo = :memberNo AND b.isCancelled = 0")
    List<Long> findDistinctProductNosByMemberNo(@Param("memberNo") Long memberNo);

    // 마이페이지: 특정 회원이 낙찰받은 상품 번호 목록
    @Query("SELECT b.productNo FROM BidHistory b WHERE b.memberNo = :memberNo AND b.isWinner = 1")
    List<Long> findWonProductNosByMemberNo(@Param("memberNo") Long memberNo);

    // 특정 회원의 특정 상품들에 대한 낙찰 여부 배치 조회
    @Query("SELECT b.productNo FROM BidHistory b WHERE b.memberNo = :memberNo AND b.productNo IN :productNos AND b.isWinner = 1")
    List<Long> findWonProductNosInList(@Param("memberNo") Long memberNo, @Param("productNos") List<Long> productNos);

    // 특정 상품의 낙찰 입찰 기록 조회 (낙찰자 확인용)
    @Query("SELECT b FROM BidHistory b WHERE b.productNo = :productNo AND b.isWinner = 1")
    Optional<BidHistory> findWinnerByProductNo(@Param("productNo") Long productNo);

    // 특정 상품의 고유 입찰자 memberNo 목록 (취소 입찰 제외)
    @Query("SELECT DISTINCT b.memberNo FROM BidHistory b WHERE b.productNo = :productNo AND b.isCancelled = 0")
    List<Long> findDistinctBiddersByProductNo(@Param("productNo") Long productNo);

    // 특정 상품의 고유 입찰자 중 특정 회원 제외 (낙찰 실패자 알림용)
    @Query("SELECT DISTINCT b.memberNo FROM BidHistory b WHERE b.productNo = :productNo AND b.isCancelled = 0 AND b.memberNo <> :excludeMemberNo")
    List<Long> findDistinctBiddersExcluding(@Param("productNo") Long productNo, @Param("excludeMemberNo") Long excludeMemberNo);
}