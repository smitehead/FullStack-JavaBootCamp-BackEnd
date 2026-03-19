package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.dto.ProductDetailResponseDto; // 별도로 생성한 DTO
import com.javajava.project.entity.Member;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.repository.ProductRepository;
// import com.javajava.project.repository.ImageRepository;    // 추가 메서드 관련 주석
// import com.javajava.project.repository.WishlistRepository; // 추가 메서드 관련 주석
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
    private final MemberRepository memberRepository;
    private final BidHistoryRepository bidHistoryRepository;
    // private final ImageRepository imageRepository;       // 추가 레포지토리 주석
    // private final WishlistRepository wishlistRepository; // 추가 레포지토리 주석

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
                .mainImageUrl("/api/images/sample.jpg")
                .build()
        ).collect(Collectors.toList());
    }

    // 신규 반영: 제품 상세 페이지를 위한 통합 조회 메서드
    @Override
    public ProductDetailResponseDto getProductDetail(Long productNo, Long currentMemberNo) {
        // 1. 상품 기본 정보 조회
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));

        // 2. 판매자 정보 조회 (닉네임, 매너온도 등) [cite: 143-146]
        Member seller = memberRepository.findById(product.getSellerNo())
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));

        // 3. 입찰 기록 조회 (최신순) [cite: 168-173]
        List<ProductDetailResponseDto.BidHistoryDto> bidHistory = bidHistoryRepository.findByProductNoOrderByBidTimeDesc(productNo)
                .stream()
                .map(bid -> {
                    String nickname = memberRepository.findById(bid.getMemberNo())
                            .map(Member::getNickname).orElse("알 수 없음");
                    return ProductDetailResponseDto.BidHistoryDto.builder()
                            .bidderNickname(nickname)
                            .bidPrice(bid.getBidPrice())
                            .bidTime(bid.getBidTime())
                            .build();
                }).collect(Collectors.toList());

        /* * 추가 메서드 및 레포지토리 미구현으로 인한 주석 처리 부분
         * // 4. 이미지 조회 (ImageRepository 구현 후 사용)
        List<String> imageUrls = imageRepository.findByProductNo(productNo)
                .stream().map(Image::getImgName).collect(Collectors.toList());

        // 5. 찜 여부 확인 (WishlistRepository 구현 후 사용)
        boolean isWishlisted = false;
        if (currentMemberNo != null) {
            isWishlisted = wishlistRepository.existsByMemberNoAndProductNo(currentMemberNo, productNo);
        }
        */

        // 6. 상세 데이터 조립
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
                // .imageUrls(imageUrls) // 주석 처리
                // .isWishlisted(isWishlisted) // 주석 처리
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
    public List<ProductResponseDto> findByCategory(Long categoryNo) {
        // 카테고리는 원래 상태처럼 미구현 상태로 유지 (전체 조회 호출) 
        return findAllActive("latest");
    }
}