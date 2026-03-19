package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Member;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final BidHistoryRepository bidHistoryRepository;

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
                .createdAt(LocalDateTime.now()) // Oracle NOT NULL 제약 조건(ORA-01400) 해결
                .viewCount(0L)
                .bidCount(0L)
                .isActive(1)
                .isDeleted(0)
                .build();

        return productRepository.save(product).getProductNo();
    }

    @Override
    public List<ProductResponseDto> findAllActive(String sortOption) {
        // 정렬 기준 설정
        Sort sort = switch (sortOption) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "ending" -> Sort.by(Sort.Direction.ASC, "endTime");
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        List<Product> products = productRepository.findByIsActiveAndIsDeleted(1, 0, sort);

        return products.stream().map(product -> ProductResponseDto.builder()
                .productNo(product.getProductNo())
                .title(product.getTitle())
                .currentPrice(product.getCurrentPrice())
                .location(product.getTradeAddrDetail())
                .endTime(product.getEndTime())
                .isActive(product.getIsActive())
                .mainImageUrl("/api/images/sample.jpg") // 이미지 엔티티 연동 전 샘플 경로
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public ProductDetailResponseDto getProductDetail(Long productNo, Long currentMemberNo) {
        // 1. 상품 기본 정보 조회
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));

        // 2. 판매자 정보 조회 (닉네임, 매너온도 등)
        Member seller = memberRepository.findById(product.getSellerNo())
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));

        // 3. 최적화된 JOIN 쿼리로 입찰 기록 조회 (N+1 문제 해결)
        List<Object[]> results = bidHistoryRepository.findBidHistoryWithNickname(productNo);
        
        List<ProductDetailResponseDto.BidHistoryDto> bidHistory = results.stream()
                .map(result -> {
                    BidHistory bid = (BidHistory) result[0];
                    String nickname = (String) result[1];
                    
                    return ProductDetailResponseDto.BidHistoryDto.builder()
                            .bidderNickname(nickname)
                            .bidPrice(bid.getBidPrice())
                            .bidTime(bid.getBidTime())
                            .build();
                }).collect(Collectors.toList());

        // 4. 상세 데이터 조립 및 반환
        return ProductDetailResponseDto.builder()
                .productNo(product.getProductNo())
                .title(product.getTitle())
                .description(product.getDescription())
                .tradeType(product.getTradeType())
                .location(product.getTradeAddrDetail())
                .startPrice(product.getStartPrice())
                .currentPrice(product.getCurrentPrice())
                .minBidUnit(product.getMinBidUnit())
                .endTime(product.getEndTime())
                .participantCount(product.getBidCount())
                .seller(ProductDetailResponseDto.SellerInfoDto.builder()
                        .sellerNo(seller.getMemberNo())
                        .nickname(seller.getNickname())
                        .mannerTemp(seller.getMannerTemp())
                        .build())
                .bidHistory(bidHistory)
                .build();
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
    public List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo) {
        // 최적화된 JOIN 쿼리로 입찰 기록과 닉네임을 한 번에 조회
        List<Object[]> results = bidHistoryRepository.findBidHistoryWithNickname(productNo);
        
        return results.stream().map(result -> {
            BidHistory bid = (BidHistory) result[0];
            String nickname = (String) result[1];
            
            return ProductDetailResponseDto.BidHistoryDto.builder()
                    .bidderNickname(nickname)
                    .bidPrice(bid.getBidPrice())
                    .bidTime(bid.getBidTime())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProductResponseDto> findByCategory(Long categoryNo) {
        // 카테고리 기능은 현재 전체 조회로 대체 (추후 구현 가능)
        return findAllActive("latest");
    }
}