package com.javajava.project.service;

import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import java.util.List;

public interface ProductService {
    // 상품 등록
    Long save(ProductRequestDto productDto);

    // 활성 상태 상품 전체 조회 (정렬 포함)
    List<ProductResponseDto> findAllActive(String sort);

    // 상품 상세 조회
    ProductResponseDto findById(Long productNo);

    // 카테고리별 상품 조회
    List<ProductResponseDto> findByCategory(Long categoryNo);

    
    ProductDetailResponseDto getProductDetail(Long productNo, Long currentMemberNo);
    
}