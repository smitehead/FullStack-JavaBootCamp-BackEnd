package com.javajava.project.repository;

import com.javajava.project.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByMemberNoAndProductNo(Long memberNo, Long productNo);
    boolean existsByMemberNoAndProductNo(Long memberNo, Long productNo);

    // 【추가 - N+1 개선】특정 회원이 찜한 상품 번호 목록을 IN 쿼리로 한 번에 조회
    @Query("SELECT w.productNo FROM Wishlist w WHERE w.memberNo = :memberNo AND w.productNo IN :productNos")
    List<Long> findWishlistedProductNos(@Param("memberNo") Long memberNo,
                                        @Param("productNos") List<Long> productNos);

    // 마이페이지: 특정 회원이 찜한 모든 상품 번호 조회
    @Query("SELECT w.productNo FROM Wishlist w WHERE w.memberNo = :memberNo")
    List<Long> findProductNosByMemberNo(@Param("memberNo") Long memberNo);
}
