package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
                .currentPrice(dto.getStartPrice()) // 초기가는 시작가와 동일
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
    public List<ProductResponseDto> findAllActive(String sortOption) {
        // 1. 프론트엔드 버튼 클릭에 따른 정렬 기준 설정
        Sort sort = switch (sortOption) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "viewCount"); // 인기순
            case "ending" -> Sort.by(Sort.Direction.ASC, "endTime");     // 종료임박순
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");  // 최신순
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        // 2. DB에서 데이터 조회 (활성 상태인 것만)
        List<Product> products = productRepository.findByIsActiveAndIsDeleted(1, 0, sort);

        // 3. Entity를 ProductResponseDto(조립 박스)로 변환
        return products.stream().map(product -> ProductResponseDto.builder()
                .productNo(product.getProductNo())
                .title(product.getTitle())
                .currentPrice(product.getCurrentPrice())
                .location(product.getTradeAddrDetail()) // 추후 주소 가공 가능
                .endTime(product.getEndTime())
                .isActive(product.getIsActive())
                .mainImageUrl("/api/images/sample.jpg") // 임시 이미지 경로
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public ProductResponseDto findById(Long productNo) {
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));
        
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
        // 기본 전체 조회 로직 호출 (추후 카테고리 필터링 추가 가능)
        return findAllActive("latest");
    }
}