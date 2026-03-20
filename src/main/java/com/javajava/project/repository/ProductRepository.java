package com.javajava.project.repository;

import com.javajava.project.entity.Product;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Sort; // 추가
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
// 입찰 중 다른 사용자가 가격을 수정하지 못하도록 락을 겁니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productNo = :productNo")
    Optional<Product> findByIdWithLock(@Param("productNo") Long productNo);

    List<Product> findBySellerNo(Long sellerNo);
    
    // Sort 매개변수를 추가하여 정렬 기능을 지원하도록 수정
    List<Product> findByIsActiveAndIsDeleted(Integer isActive, Integer isDeleted, Sort sort);

    // 종료 시간이 현재 시간보다 이전이면서 여전히 활성 상태인 상품 조회
    List<Product> findByEndTimeBeforeAndIsActive(LocalDateTime now, Integer isActive);
}