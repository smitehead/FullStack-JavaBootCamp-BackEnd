package com.javajava.project.repository;

import com.javajava.project.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    
    // 특정 상품의 모든 이미지 조회 (대표 사진이 먼저 오도록 정렬)
    List<ProductImage> findByProductNoOrderByIsMainDesc(Long productNo);
    
    // 특정 상품의 대표 이미지 1개 조회
    ProductImage findFirstByProductNoAndIsMainOrderByImageNoAsc(Long productNo, Integer isMain);
}
