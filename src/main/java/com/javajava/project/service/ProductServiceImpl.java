package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long save(ProductRequestDto dto) {
        // DTO -> Entity 변환 및 초기 비즈니스 로직 적용
        Product product = Product.builder()
                .sellerNo(dto.getSellerNo())
                .categoryNo(dto.getCategoryNo())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .tradeType(dto.getTradeType())
                .tradeEmdNo(dto.getTradeEmdNo())
                .tradeAddrDetail(dto.getTradeAddrDetail())
                .startPrice(dto.getStartPrice())
                .currentPrice(dto.getStartPrice()) // 초기 현재가는 시작가와 동일하게 설정
                .buyoutPrice(dto.getBuyoutPrice())
                .minBidUnit(dto.getMinBidUnit())
                .endTime(dto.getEndTime())
                .viewCount(0L) // 초기 조회수 0
                .bidCount(0L)  // 초기 입찰수 0
                .isActive(1)   // 등록 시 바로 활성화
                .isDeleted(0)  // 삭제 여부 미삭제(0)
                .build();

        return productRepository.save(product).getProductNo();
    }

    @Override
    public List<Product> findAllActive() {
        // 활성 상태(1)이고 삭제되지 않은(0) 상품만 조회
        return productRepository.findByIsActiveAndIsDeleted(1, 0);
    }

    @Override
    public Product findById(Long productNo) {
        // 상품 번호로 상세 정보 조회, 없을 경우 예외 발생
        return productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));
    }

    @Override
    public List<Product> findByCategory(Long categoryNo) {
        // 카테고리별 필터 조회 기능 (Repository에 메서드 추가 필요)
        return productRepository.findAll(); 
    }
}