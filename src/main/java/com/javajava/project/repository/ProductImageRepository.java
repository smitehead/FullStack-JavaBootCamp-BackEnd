package com.javajava.project.repository;

import com.javajava.project.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // 단일 상품의 모든 이미지 조회 (대표 사진 우선 정렬)
    List<ProductImage> findByProductNoOrderByIsMainDesc(Long productNo);

    // 단일 상품의 대표 이미지 1개 조회
    ProductImage findFirstByProductNoAndIsMainOrderByImageNoAsc(Long productNo, Integer isMain);

    // 【추가 - N+1 개선】여러 상품의 메인 이미지를 IN 쿼리로 한 번에 조회
    @Query("SELECT pi FROM ProductImage pi WHERE pi.productNo IN :productNos AND pi.isMain = 1")
    List<ProductImage> findMainImagesByProductNos(@Param("productNos") List<Long> productNos);
}
