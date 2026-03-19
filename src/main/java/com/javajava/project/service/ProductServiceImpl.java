package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long save(ProductRequestDto dto) {
        Product product = Product.builder()
                .sellerNo(dto.getSellerNo())
                .categoryNo(dto.getCategoryNo())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .tradeType(dto.getTradeType())
                .tradeEmdNo(dto.getTradeEmdNo())
                .tradeAddrDetail(dto.getTradeAddrDetail())
                .startPrice(dto.getStartPrice())
                .currentPrice(dto.getStartPrice())
                .buyoutPrice(dto.getBuyoutPrice())
                .minBidUnit(dto.getMinBidUnit())
                .endTime(dto.getEndTime())
                .viewCount(0L)
                .bidCount(0L)
                .isActive(1)
                .isDeleted(0)
                .build();

        return productRepository.save(product).getProductNo();
    }

    @Override
    public List<ProductResponseDto> findAllActive() {
        // 1. DB에서 활성 상품 엔티티들을 가져옴
        List<Product> products = productRepository.findByIsActiveAndIsDeleted(1, 0);

        // 2. 엔티티를 DTO로 변환(조립)하여 리스트로 반환
        return products.stream().map(product -> ProductResponseDto.builder()
                .productNo(product.getProductNo())
                .title(product.getTitle())                      // [상품이름]
                .currentPrice(product.getCurrentPrice())        // [현재 최고가]
                .location(product.getTradeAddrDetail())         // [주소]
                .endTime(product.getEndTime())                  // [남은시간 계산용]
                .isActive(product.getIsActive())
                .mainImageUrl("/api/images/sample.jpg")         // [상품사진] 임시 경로
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public ProductResponseDto findById(Long productNo) {
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));
        
        // 엔티티를 DTO로 변환하여 반환
        return ProductResponseDto.builder()
                .productNo(product.getProductNo())
                .title(product.getTitle())
                .currentPrice(product.getCurrentPrice())
                .location(product.getTradeAddrDetail())
                .endTime(product.getEndTime())
                .isActive(product.getIsActive())
                .mainImageUrl("/api/images/sample.jpg")
                .build();
    }

    @Override
    public List<ProductResponseDto> findByCategory(Long categoryNo) {
        // 카테고리 필터 조회 로직 (추후 Repository 확장 필요)
        return findAllActive(); 
    }
}