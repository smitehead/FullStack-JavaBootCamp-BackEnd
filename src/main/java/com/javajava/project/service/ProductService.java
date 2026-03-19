package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto; 
import java.util.List;

public interface ProductService {
    // 상품 등록
    Long save(ProductRequestDto productDto);

    // 활성 상태 상품 전체 조회 (DTO 리스트 반환)
    List<ProductResponseDto> findAllActive();

    // 상품 상세 조회 (DTO 반환)
    ProductResponseDto findById(Long productNo);

    // 카테고리별 상품 조회
    List<ProductResponseDto> findByCategory(Long categoryNo);
}