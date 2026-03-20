package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.dto.ProductDetailResponseDto; // 추가
import java.util.List;

public interface ProductService {
    Long save(ProductRequestDto productDto);

    List<ProductResponseDto> findAllActive(String sort);

    // 상세 조회 메서드 추가 (기존 findById와 별개로 상세 페이지 전용)
    ProductDetailResponseDto getProductDetail(Long productNo, Long currentMemberNo);

    // 추가: 입찰 기록만 별도로 조회하는 메서드
    List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo);

    ProductResponseDto findById(Long productNo);

    List<ProductResponseDto> findByCategory(Long categoryNo);
}