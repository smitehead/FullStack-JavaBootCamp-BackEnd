package com.javajava.project.service;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.dto.ProductListResponseDto;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Member;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.repository.ProductRepository;
import com.javajava.project.entity.ProductImage;
import com.javajava.project.repository.ProductImageRepository;
import com.javajava.project.repository.WishlistRepository;
import com.javajava.project.util.FileStore;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final ProductImageRepository productImageRepository;
    private final WishlistRepository wishlistRepository;
    private final FileStore fileStore;

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
                .status(0)      // [수정] isActive(1) → status(0=active)
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

        // [수정] findByIsActiveAndIsDeleted(1, 0) → findByStatusAndIsDeleted(0, 0)
        List<Product> products = productRepository.findByStatusAndIsDeleted(0, 0, sort);

        return products.stream().map(product -> {
            ProductImage mainImage = productImageRepository.findFirstByProductNoAndIsMainOrderByImageNoAsc(product.getProductNo(), 1);
            String imageUrl = mainImage != null ? "/api/images/" + mainImage.getUuidName() : null;
            return ProductResponseDto.builder()
                .productNo(product.getProductNo())
                .title(product.getTitle())
                .currentPrice(product.getCurrentPrice())
                .location(product.getTradeAddrDetail())
                .endTime(product.getEndTime())
                .status(product.getStatus())    // [수정] isActive → status
                .mainImageUrl(imageUrl)
                .build();
        }).collect(Collectors.toList());
    }

    @Override
    public Page<ProductListResponseDto> getProductList(int page, int size, Long large, Long medium, Long small,
                                                       Long minPrice, Long maxPrice, String city,
                                                       Boolean delivery, Boolean face, String sortOption, Long memberNo) {

        // 리액트는 1페이지부터 보내므로 백엔드용(0-index)으로 보정
        int pageNumber = page > 0 ? page - 1 : 0;

        // 프론트엔드 정렬 조건 매핑
        // - popular(인기순): 입찰 횟수 많은 순 → 조회수 많은 순 (복합 정렬)
        // - ending(종료임박순): 경매 종료 시간이 가장 가까운 것 먼저
        // - latest(최신순): 등록 날짜 최근 순
        // - all / 기타: 최신순과 동일 (기본값)
        Sort sort = switch (sortOption != null ? sortOption : "latest") {
            case "popular" -> Sort.by(
                    Sort.Order.desc("bidCount"),   // 1순위: 입찰 수 많은 것
                    Sort.Order.desc("viewCount")   // 2순위: 조회수 많은 것 (동점 시)
            );
            case "ending" -> Sort.by(Sort.Order.asc("endTime"));   // 종료 임박 순
            case "latest" -> Sort.by(Sort.Order.desc("createdAt")); // 최신 등록 순
            default -> Sort.by(Sort.Order.desc("createdAt"));       // all 또는 기타 → 최신순
        };

        Pageable pageable = PageRequest.of(pageNumber, size, sort);

        // 동적 쿼리 (다중 필터 적용)
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // [수정] isActive=1 → status=0 (진행 중인 상품)
            predicates.add(cb.equal(root.get("status"), 0));
            predicates.add(cb.equal(root.get("isDeleted"), 0)); // 삭제 안 된 상품

            // 종료된 경매 제외: endTime이 현재 시각보다 미래인 것만 조회
            predicates.add(cb.greaterThan(root.get("endTime"), LocalDateTime.now()));

            // 카테고리 필터 (임시: 실제로는 Category 테이블 JOIN 필요)
            if (small != null) predicates.add(cb.equal(root.get("categoryNo"), small));
            else if (medium != null) predicates.add(cb.equal(root.get("categoryNo"), medium));

            // 가격 필터
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("currentPrice"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("currentPrice"), maxPrice));

            // 지역 필터 (임시: EMD 테이블 JOIN 대신 주소 텍스트 검색)
            if (city != null && !city.isEmpty()) predicates.add(cb.like(root.get("tradeAddrDetail"), "%" + city + "%"));

            // 거래 방식 필터 (택배, 대면)
            if (Boolean.TRUE.equals(delivery) && Boolean.TRUE.equals(face)) predicates.add(cb.in(root.get("tradeType")).value(Arrays.asList("택배거래", "직거래", "혼합")));
            else if (Boolean.TRUE.equals(delivery)) predicates.add(cb.in(root.get("tradeType")).value(Arrays.asList("택배거래", "혼합")));
            else if (Boolean.TRUE.equals(face)) predicates.add(cb.in(root.get("tradeType")).value(Arrays.asList("직거래", "혼합")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // N+1 제거: 먼저 페이지 데이터를 가져온 후 배치 쿼리로 연관 데이터를 일괄 조회
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<Long> productNos = productPage.getContent().stream()
                .map(Product::getProductNo)
                .collect(Collectors.toList());

        // 메인 이미지 배치 조회 (상품 수 만큼 쿼리 → 1번 IN 쿼리)
        Map<Long, ProductImage> mainImageMap = productNos.isEmpty() ?
                Map.of() :
                productImageRepository.findMainImagesByProductNos(productNos).stream()
                        .collect(Collectors.toMap(ProductImage::getProductNo, Function.identity(),
                                (a, b) -> a)); // 중복 시 첫 번째 유지

        // 찜 여부 배치 조회 (상품 수 만큼 쿼리 → 1번 IN 쿼리)
        Set<Long> wishlistedNos = (memberNo != null && !productNos.isEmpty())
                ? Set.copyOf(wishlistRepository.findWishlistedProductNos(memberNo, productNos))
                : Set.of();

        return productPage.map(product -> {
            ProductImage mainImg = mainImageMap.get(product.getProductNo());
            List<String> imageUrls = mainImg != null
                    ? List.of("/api/images/" + mainImg.getUuidName())
                    : List.of();

            return ProductListResponseDto.builder()
                    .id(product.getProductNo())
                    .title(product.getTitle())
                    .location(product.getTradeAddrDetail())
                    .currentPrice(product.getCurrentPrice())
                    .endTime(product.getEndTime())
                    .participantCount(bidHistoryRepository.countDistinctParticipants(product.getProductNo()))
                    .status(product.getEndTime().isBefore(LocalDateTime.now()) ? "completed" : "active")
                    .images(imageUrls)
                    .isWishlisted(wishlistedNos.contains(product.getProductNo()))
                    .build();
        });
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

        // 4. 이미지 정보 조회
        List<ProductImage> productImages = productImageRepository.findByProductNoOrderByIsMainDesc(productNo);
        List<String> imageUrls = productImages.stream()
                .map(img -> "/api/images/" + img.getUuidName())
                .collect(Collectors.toList());

        // 5. 상세 데이터 조립 및 반환
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
                .participantCount(bidHistoryRepository.countDistinctParticipants(product.getProductNo()))
                .images(imageUrls)
                .isWishlisted(currentMemberNo != null ? wishlistRepository.existsByMemberNoAndProductNo(currentMemberNo, product.getProductNo()) : false)
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

        ProductImage mainImage = productImageRepository.findFirstByProductNoAndIsMainOrderByImageNoAsc(productNo, 1);
        String imageUrl = mainImage != null ?
                "/api/images/" + mainImage.getUuidName() : null;

        return ProductResponseDto.builder()
                .productNo(product.getProductNo())
                .title(product.getTitle())
                .currentPrice(product.getCurrentPrice())
                .location(product.getTradeAddrDetail())
                .endTime(product.getEndTime())
                .status(product.getStatus())    // [수정] isActive → status
                .mainImageUrl(imageUrl)
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

    @Override
    @Transactional
    public void saveImages(Long productNo, List<MultipartFile> images) throws java.io.IOException {
        List<ProductImage> storedImages = fileStore.storeFiles(images, productNo);
        if (!storedImages.isEmpty()) {
            productImageRepository.saveAll(storedImages);
        }
    }
}