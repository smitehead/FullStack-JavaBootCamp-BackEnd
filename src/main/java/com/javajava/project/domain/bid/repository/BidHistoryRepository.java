package com.javajava.project.domain.bid.repository;

import com.javajava.project.domain.bid.entity.BidHistory;
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

    // 상세 페이지 입찰 기록 조회: 취소된 입찰 제외 + 취소 이력이 있는 회원의 모든 입찰 제외
    @Query("SELECT b, m.nickname FROM BidHistory b " +
           "JOIN Member m ON b.memberNo = m.memberNo " +
           "WHERE b.productNo = :productNo AND b.isCancelled = 0 " +
           "AND b.memberNo NOT IN (" +
           "  SELECT b2.memberNo FROM BidHistory b2 " +
           "  WHERE b2.productNo = :productNo AND b2.isCancelled = 1" +
           ") " +
           "ORDER BY b.bidTime DESC")
    List<Object[]> findBidHistoryWithNickname(@Param("productNo") Long productNo);

    // 재입찰 차단 검증: 해당 상품에 취소 이력이 있는지 확인
    boolean existsByProductNoAndMemberNoAndIsCancelled(Long productNo, Long memberNo, Integer isCancelled);

    // 특정 상품에 입찰한 고유 회원 수 조회
    @Query("SELECT COUNT(DISTINCT b.memberNo) FROM BidHistory b WHERE b.productNo = :productNo AND b.isCancelled = 0")
    Long countDistinctParticipants(@Param("productNo") Long productNo);

    // 여러 상품의 참여자 수를 한 번에 조회 (N+1 방지)
    @Query("SELECT b.productNo, COUNT(DISTINCT b.memberNo) FROM BidHistory b WHERE b.productNo IN :productNos AND b.isCancelled = 0 GROUP BY b.productNo")
    List<Object[]> countDistinctParticipantsByProductNos(@Param("productNos") List<Long> productNos);

    // 마이페이지: 특정 회원이 입찰한 고유 상품 번호 목록
    // 취소 이력(isCancelled=1)이 있는 상품은 아예 제외 — 취소한 경매가 마이페이지에 노출되는 버그 방지
    @Query("SELECT b.productNo FROM BidHistory b " +
           "WHERE b.memberNo = :memberNo AND b.isCancelled = 0 " +
           "AND b.productNo NOT IN (" +
           "  SELECT b2.productNo FROM BidHistory b2 " +
           "  WHERE b2.memberNo = :memberNo AND b2.isCancelled = 1" +
           ") " +
           "GROUP BY b.productNo " +
           "ORDER BY MAX(b.bidTime) DESC")
    List<Long> findDistinctProductNosByMemberNo(@Param("memberNo") Long memberNo);

    // 마이페이지: 특정 회원이 낙찰받은 상품 번호 목록
    @Query("SELECT b.productNo FROM BidHistory b WHERE b.memberNo = :memberNo AND b.isWinner = 1 ORDER BY b.bidTime DESC")
    List<Long> findWonProductNosByMemberNo(@Param("memberNo") Long memberNo);

    // 특정 회원의 특정 상품들에 대한 낙찰 여부 배치 조회
    @Query("SELECT b.productNo FROM BidHistory b WHERE b.memberNo = :memberNo AND b.productNo IN :productNos AND b.isWinner = 1")
    List<Long> findWonProductNosInList(@Param("memberNo") Long memberNo, @Param("productNos") List<Long> productNos);

    // 특정 상품의 낙찰 입찰 기록 조회 (낙찰자 확인용)
    // findFirst: 중복 낙찰 처리 시 NonUniqueResultException 방지
    Optional<BidHistory> findFirstByProductNoAndIsWinnerOrderByBidPriceDesc(Long productNo, Integer isWinner);

    /**
     * 차순위 후보 목록 조회 — 특정 회원을 완전히 제외.
     * <p>입찰 취소자의 모든 입찰을 건너뛰기 위해 memberNo 전체 제외 조건 적용.
     * isCancelled=0 인 유효한 입찰 중 해당 회원 제외, 입찰가 내림차순 정렬.
     *
     * @param productNo        대상 상품 번호
     * @param isCancelled      0 = 유효 입찰만
     * @param excludeMemberNo  제외할 회원번호 (취소한 회원)
     */
    List<BidHistory> findByProductNoAndIsCancelledAndMemberNoNotOrderByBidPriceDesc(
            Long productNo, Integer isCancelled, Long excludeMemberNo);

    // 특정 상품의 고유 입찰자 memberNo 목록 (취소 입찰 제외)
    @Query("SELECT DISTINCT b.memberNo FROM BidHistory b WHERE b.productNo = :productNo AND b.isCancelled = 0")
    List<Long> findDistinctBiddersByProductNo(@Param("productNo") Long productNo);

    // 특정 상품의 고유 입찰자 중 특정 회원 제외 (낙찰 실패자 알림용)
    @Query("SELECT DISTINCT b.memberNo FROM BidHistory b WHERE b.productNo = :productNo AND b.isCancelled = 0 AND b.memberNo <> :excludeMemberNo")
    List<Long> findDistinctBiddersExcluding(@Param("productNo") Long productNo, @Param("excludeMemberNo") Long excludeMemberNo);

    // 마이페이지: 여러 상품의 현재 최고입찰자(memberNo) 배치 조회 (초기 bidStatus 판별용)
    @Query("SELECT b.productNo, b.memberNo FROM BidHistory b " +
           "WHERE b.productNo IN :productNos AND b.isCancelled = 0 " +
           "AND b.bidPrice = (SELECT MAX(b2.bidPrice) FROM BidHistory b2 " +
           "                  WHERE b2.productNo = b.productNo AND b2.isCancelled = 0)")
    List<Object[]> findTopBidderByProductNos(@Param("productNos") List<Long> productNos);

    // 특정 회원의 특정 상품 리스트에 대한 모든 입찰 내역 조회
    List<BidHistory> findByMemberNoAndProductNoIn(Long memberNo, List<Long> productNos);
}