package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.entity.Product;
import java.util.List;

public interface ProductService {
    // 상품 등록 (DTO 사용)
    Long save(ProductRequestDto productDto);

    // 활성 상태 상품 전체 조회
    List<Product> findAllActive();

    // 상품 상세 조회
    Product findById(Long productNo);

    // 카테고리별 상품 조회
    List<Product> findByCategory(Long categoryNo);
}