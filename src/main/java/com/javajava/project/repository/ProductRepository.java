package com.javajava.project.repository;

import com.javajava.project.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 판매자별 상품 목록 조회
    List<Product> findBySellerNo(Long sellerNo);
    
    // 진행 중인 경매만 조회
    List<Product> findByIsActiveAndIsDeleted(Integer isActive, Integer isDeleted);
}