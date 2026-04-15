package com.javajava.project.domain.product.repository;

import com.javajava.project.domain.product.entity.Product;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // 입찰 중 다른 사용자가 가격을 수정하지 못하도록 락 적용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productNo = :productNo")
    Optional<Product> findByIdWithLock(@Param("productNo") Long productNo);

    List<Product> findBySellerNoOrderByProductNoDesc(Long sellerNo);

    // 진행 중(status=0)이고 삭제되지 않은 상품 조회 (정렬 지원)
    List<Product> findByStatusAndIsDeleted(Integer status, Integer isDeleted, Sort sort);

    // 종료 시간이 지났지만 아직 진행 중(status=0)인 상품 조회 → 스케줄러용
    List<Product> findByEndTimeBeforeAndStatus(LocalDateTime now, Integer status);

    // 관리자용: 삭제되지 않은 전체 상품 조회 (최신순)
    List<Product> findByIsDeletedOrderByCreatedAtDesc(Integer isDeleted);

    // Watchdog 서버 재시작 복구용: 아직 종료 안 된 경매 중 상품 조회
    List<Product> findByStatusAndEndTimeAfter(Integer status, LocalDateTime now);

    // Phase2 스케줄러: 결제 대기 중인 상품 조회 (status=3)
    List<Product> findByStatus(Integer status);

    long countBySellerNoAndStatusAndIsDeleted(Long sellerNo, Integer status, Integer isDeleted);

    long countBySellerNoAndIsDeleted(Long sellerNo, Integer isDeleted);

    // 관리자 대시보드: 대분류별 상품 건수 집계 (depth 1/2/3 모두 대응)
    @Query(value = """
            SELECT
                CASE
                    WHEN c.DEPTH = 1 THEN c.NAME
                    WHEN c.DEPTH = 2 THEN c2.NAME
                    WHEN c.DEPTH = 3 THEN c3.NAME
                END AS name,
                COUNT(p.PRODUCT_NO) AS cnt
            FROM PRODUCT p
            JOIN CATEGORY c ON p.CATEGORY_NO = c.CATEGORY_NO
            LEFT JOIN CATEGORY c2 ON c.PARENT_NO = c2.CATEGORY_NO
            LEFT JOIN CATEGORY c3 ON c2.PARENT_NO = c3.CATEGORY_NO
            WHERE p.IS_DELETED = 0
            GROUP BY
                CASE
                    WHEN c.DEPTH = 1 THEN c.NAME
                    WHEN c.DEPTH = 2 THEN c2.NAME
                    WHEN c.DEPTH = 3 THEN c3.NAME
                END
            """, nativeQuery = true)
    List<Object[]> countProductsByRootCategory();
}