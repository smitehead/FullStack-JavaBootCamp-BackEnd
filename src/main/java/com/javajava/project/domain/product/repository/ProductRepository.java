package com.javajava.project.domain.product.repository;

import com.javajava.project.domain.product.entity.Product;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * 입찰 트랜잭션 전용 비관적 락 조회.
     *
     * <p>{@code SELECT ... FOR UPDATE} 를 발행하여 동일 행에 대한 경쟁 트랜잭션을
     * 직렬화한다. 락 대기 타임아웃은 3 초로 제한하여 무한 블로킹을 방지한다.
     *
     * <p><b>주의:</b> 반드시 활성 읽기-쓰기 트랜잭션 내에서 호출해야 하며,
     * 이 메서드가 트랜잭션의 <em>첫 번째 쿼리</em>여야 한다.
     * 다른 쿼리 실행 후 호출하면 MVCC 스냅샷이 이미 설정되어
     * 락 획득 시점과 가격 검증 시점 사이에 불일치가 생길 수 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
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