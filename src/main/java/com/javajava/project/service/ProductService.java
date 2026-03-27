package com.javajava.project.service;

import com.javajava.project.dto.AdminProductResponseDto;
import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.dto.ProductDetailResponseDto; // 추가
import com.javajava.project.dto.ProductListResponseDto;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ProductService {
    Long save(ProductRequestDto productDto);

    List<ProductResponseDto> findAllActive(String sort);

    // 상품 목록 동적 필터링 및 페이징
    Page<ProductListResponseDto> getProductList(int page, int size, Long large, Long medium, Long small,
                                                Long minPrice, Long maxPrice, String city,
                                                Boolean delivery, Boolean face, String sort, Long memberNo);

    // 상세 조회 메서드 추가 (기존 findById와 별개로 상세 페이지 전용)
    ProductDetailResponseDto getProductDetail(Long productNo, Long currentMemberNo);

    // 추가: 입찰 기록만 별도로 조회하는 메서드
    List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo);

    ProductResponseDto findById(Long productNo);

    List<ProductResponseDto> findByCategory(Long categoryNo);

    void saveImages(Long productNo, List<org.springframework.web.multipart.MultipartFile> images) throws java.io.IOException;

    // 마이페이지: 내가 등록한 상품 목록
    List<ProductListResponseDto> getMySellingProducts(Long memberNo);

    // 마이페이지: 내가 입찰한 상품 목록 (입찰상태 포함)
    List<ProductListResponseDto> getMyBiddingProducts(Long memberNo);

    // 마이페이지: 구매 완료(구매확정) 상품 목록
    List<ProductListResponseDto> getMyPurchasedProducts(Long memberNo);

    // 마이페이지: 내 찜 목록
    List<ProductListResponseDto> getMyWishlistProducts(Long memberNo);

    // 상품 삭제 (soft delete)
    void deleteProduct(Long productNo, Long memberNo);

    // 관리자: 전체 상품 목록 (삭제 제외)
    List<AdminProductResponseDto> getAllProductsForAdmin();

    // 관리자: 경매 강제 종료
    void cancelAuctionByAdmin(Long productNo);
}