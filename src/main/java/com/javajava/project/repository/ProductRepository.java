package com.javajava.project.repository;

import com.javajava.project.entity.Product;
import org.springframework.data.domain.Sort; // 추가
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySellerNo(Long sellerNo);
    
    // Sort 매개변수를 추가하여 정렬 기능을 지원하도록 수정
    List<Product> findByIsActiveAndIsDeleted(Integer isActive, Integer isDeleted, Sort sort);
}