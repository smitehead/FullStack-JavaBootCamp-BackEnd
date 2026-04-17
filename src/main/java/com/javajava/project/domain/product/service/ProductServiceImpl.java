package com.javajava.project.domain.product.service;

import com.javajava.project.domain.admin.dto.AdminProductResponseDto;
import com.javajava.project.domain.product.dto.ProductRequestDto;
import com.javajava.project.domain.product.dto.ProductResponseDto;
import com.javajava.project.domain.product.dto.ProductDetailResponseDto;
import com.javajava.project.domain.product.dto.ProductListResponseDto;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.product.repository.ProductRepository;
import com.javajava.project.domain.product.entity.ProductImage;
import com.javajava.project.domain.product.entity.Category;
import com.javajava.project.domain.product.repository.CategoryRepository;
import com.javajava.project.domain.product.repository.ProductImageRepository;
import com.javajava.project.domain.wishlist.repository.WishlistRepository;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.auction.scheduler.AuctionExpiryWatchdog;
import com.javajava.project.global.util.FileStore;
import com.javajava.project.global.sse.SseService;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.domain.member.entity.MannerHistory;
import com.javajava.project.domain.member.repository.MannerHistoryRepository;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.bid.entity.AutoBid;
import com.javajava.project.domain.bid.repository.AutoBidRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

        private final ProductRepository productRepository;
        private final MemberRepository memberRepository;
        private final BidHistoryRepository bidHistoryRepository;
        private final ProductImageRepository productImageRepository;
        private final WishlistRepository wishlistRepository;
        private final AuctionResultRepository auctionResultRepository;
        private final FileStore fileStore;
        private final NotificationService notificationService;
        private final AuctionExpiryWatchdog auctionExpiryWatchdog;
        private final CategoryRepository categoryRepository;
        private final MannerHistoryRepository mannerHistoryRepository;
        private final PointHistoryRepository pointHistoryRepository;
        private final AutoBidRepository autoBidRepository;
        private final SseService sseService;

        @Override
        @Transactional
        public Long save(ProductRequestDto dto) {
                Product product = Product.builder()
                                .sellerNo(dto.getSellerNo())
                                .categoryNo(dto.getCategoryNo())
                                .title(dto.getTitle())
                                .description(dto.getDescription())
                                .tradeType(dto.getTradeType())
                                .tradeAddrDetail(dto.getTradeAddrDetail())
                                .tradeAddrShort(dto.getTradeAddrShort())
                                .startPrice(dto.getStartPrice())
                                .currentPrice(dto.getStartPrice()) // 초기가는 시작가와 동일
                                .buyoutPrice(dto.getBuyoutPrice())
                                .minBidUnit(dto.getMinBidUnit())
                                .endTime(dto.getEndTime())
                                .shippingFee(dto.getShippingFee() != null ? dto.getShippingFee() : 0L)
                                .createdAt(LocalDateTime.now()) // Oracle NOT NULL 제약 조건(ORA-01400) 해결
                                .viewCount(0L)
                                .bidCount(0L)
                                .status(0) // [수정] isActive(1) → status(0=active)
                                .isDeleted(0)
                                .build();

                Long productNo = productRepository.save(product).getProductNo();

                // endTime에 낙찰 처리 예약
                auctionExpiryWatchdog.scheduleClose(productNo, dto.getEndTime());

                return productNo;
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
                        ProductImage mainImage = productImageRepository
                                        .findFirstByProductNoAndIsMainOrderByImageNoAsc(product.getProductNo(), 1);
                        String imageUrl = mainImage != null ? "/api/images/" + mainImage.getUuidName() : null;
                        return ProductResponseDto.builder()
                                        .productNo(product.getProductNo())
                                        .title(product.getTitle())
                                        .currentPrice(product.getCurrentPrice())
                                        .location(product.getTradeAddrShort() != null ? product.getTradeAddrShort()
                                                        : product.getTradeAddrDetail())
                                        .endTime(product.getEndTime())
                                        .status(product.getStatus()) // [수정] isActive → status
                                        .mainImageUrl(imageUrl)
                                        .build();
                }).toList();
        }

        @Override
        public Page<ProductListResponseDto> getProductList(int page, int size, Long large, Long medium, Long small,
                        Long minPrice, Long maxPrice, String city, String district, String neighborhood,
                        Boolean delivery, Boolean face, String sortOption, String keyword, Long memberNo) {

                // 리액트는 1페이지부터 보내므로 백엔드용(0-index)으로 보정
                int pageNumber = page > 0 ? page - 1 : 0;

                // 프론트엔드 정렬 조건 매핑
                // - popular(인기순): 입찰 횟수 많은 순 → 조회수 많은 순 (복합 정렬)
                // - ending(종료임박순): 경매 종료 시간이 가장 가까운 것 먼저
                // - latest(최신순): 등록 날짜 최근 순
                // - all / 기타: 최신순과 동일 (기본값)
                Sort sort = switch (sortOption != null ? sortOption : "latest") {
                        case "popular" -> Sort.by(
                                        Sort.Order.desc("bidCount"), // 1순위: 입찰 수 많은 것
                                        Sort.Order.desc("viewCount") // 2순위: 조회수 많은 것 (동점 시)
                                );
                        case "ending" -> Sort.by(Sort.Order.asc("endTime")); // 종료 임박 순
                        case "latest" -> Sort.by(Sort.Order.desc("createdAt")); // 최신 등록 순
                        default -> Sort.by(Sort.Order.desc("createdAt")); // all 또는 기타 → 최신순
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

                        // 키워드 검색 (제목 LIKE 검색) - 대소문자 무시
                        if (keyword != null && !keyword.trim().isEmpty()) {
                                predicates.add(cb.like(
                                                cb.lower(root.get("title")),
                                                "%" + keyword.trim().toLowerCase() + "%"));
                        }

                        // 카테고리 필터 (패딩 기반 계층 구조 최적화)
                        if (small != null) {
                                // 소분류가 지정된 경우 정확히 해당 카테고리만 조회
                                predicates.add(cb.equal(root.get("categoryNo"), small));
                        } else if (medium != null) {
                                // 중분류(M) 검색 시: M 본인 및 하위 소분류(M*100 ~ M*100+99) 포함
                                predicates.add(cb.or(
                                                cb.equal(root.get("categoryNo"), medium),
                                                cb.and(
                                                                cb.greaterThanOrEqualTo(root.get("categoryNo"),
                                                                                medium * 100L),
                                                                cb.lessThan(root.get("categoryNo"),
                                                                                (medium + 1) * 100L))));
                        } else if (large != null) {
                                // 대분류(L) 검색 시: L 본인, 하위 중분류(Lxx), 하위 소분류(Lxxxx) 모두 포함
                                predicates.add(cb.or(
                                                cb.equal(root.get("categoryNo"), large),
                                                cb.and(
                                                                cb.greaterThanOrEqualTo(root.get("categoryNo"),
                                                                                large * 100L),
                                                                cb.lessThan(root.get("categoryNo"),
                                                                                (large + 1) * 100L)),
                                                cb.and(
                                                                cb.greaterThanOrEqualTo(root.get("categoryNo"),
                                                                                large * 10000L),
                                                                cb.lessThan(root.get("categoryNo"),
                                                                                (large + 1) * 10000L))));
                        }

                        // 가격 필터
                        if (minPrice != null)
                                predicates.add(cb.greaterThanOrEqualTo(root.get("currentPrice"), minPrice));
                        if (maxPrice != null)
                                predicates.add(cb.lessThanOrEqualTo(root.get("currentPrice"), maxPrice));

                        // 지역 필터 (3단계: 시/도, 시/군/구, 읍/면/동)
                        // tradeAddrShort는 "서울 강남구 역삼동" 형식으로 저장되어 있음
                        if (city != null && !city.trim().isEmpty()) {
                                predicates.add(cb.like(root.get("tradeAddrShort"), city + "%"));
                        }
                        if (district != null && !district.trim().isEmpty()) {
                                predicates.add(cb.like(root.get("tradeAddrShort"), "%" + district + "%"));
                        }
                        if (neighborhood != null && !neighborhood.trim().isEmpty()) {
                                predicates.add(cb.like(root.get("tradeAddrShort"), "%" + neighborhood + "%"));
                        }

                        // 거래 방식 필터 (AND: 둘 다 체크 시 혼합만)
                        if (Boolean.TRUE.equals(delivery) && Boolean.TRUE.equals(face))
                                predicates.add(cb.equal(root.get("tradeType"), "혼합"));
                        else if (Boolean.TRUE.equals(delivery))
                                predicates.add(cb.in(root.get("tradeType")).value(Arrays.asList("택배거래", "혼합")));
                        else if (Boolean.TRUE.equals(face))
                                predicates.add(cb.in(root.get("tradeType")).value(Arrays.asList("직거래", "혼합")));

                        return cb.and(predicates.toArray(new Predicate[0]));
                };

                // N+1 제거: 먼저 페이지 데이터를 가져온 후 배치 쿼리로 연관 데이터를 일괄 조회
                Page<Product> productPage = productRepository.findAll(spec, pageable);
                List<Long> productNos = productPage.getContent().stream()
                                .map(Product::getProductNo)
                                .toList();

                // 메인 이미지 배치 조회 (상품 수 만큼 쿼리 → 1번 IN 쿼리)
                Map<Long, ProductImage> mainImageMap = productNos.isEmpty() ? Map.of()
                                : productImageRepository.findMainImagesByProductNos(productNos).stream()
                                                .collect(Collectors.toMap(ProductImage::getProductNo,
                                                                Function.identity(),
                                                                (a, b) -> a)); // 중복 시 첫 번째 유지

                // 찜 여부 배치 조회 (상품 수 만큼 쿼리 → 1번 IN 쿼리)
                Set<Long> wishlistedNos = (memberNo != null && !productNos.isEmpty())
                                ? Set.copyOf(wishlistRepository.findWishlistedProductNos(memberNo, productNos))
                                : Set.of();

                // 참여자 수 배치 조회 (N+1 제거: 상품 수만큼 쿼리 → 1번 IN 쿼리)
                Map<Long, Long> participantCountMap = productNos.isEmpty() ? Map.of()
                                : bidHistoryRepository.countDistinctParticipantsByProductNos(productNos).stream()
                                                .collect(Collectors.toMap(
                                                                row -> (Long) row[0],
                                                                row -> (Long) row[1]));

                return productPage.map(product -> {
                        ProductImage mainImg = mainImageMap.get(product.getProductNo());
                        List<String> imageUrls = mainImg != null
                                        ? List.of("/api/images/" + mainImg.getUuidName())
                                        : List.of();

                        return ProductListResponseDto.builder()
                                        .id(product.getProductNo())
                                        .title(product.getTitle())
                                        .location(product.getTradeAddrShort() != null ? product.getTradeAddrShort()
                                                        : product.getTradeAddrDetail())
                                        .currentPrice(product.getCurrentPrice())
                                        .endTime(product.getEndTime())
                                        .participantCount(participantCountMap.getOrDefault(product.getProductNo(), 0L))
                                        .status(product.getEndTime().isBefore(LocalDateTime.now()) ? "completed"
                                                        : "active")
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
                                }).toList();

                // 4. 이미지 정보 조회
                List<ProductImage> productImages = productImageRepository.findByProductNoOrderByIsMainDesc(productNo);
                List<String> imageUrls = productImages.stream()
                                .map(img -> "/api/images/" + img.getUuidName())
                                .toList();

                // 5. 카테고리 계층 정보(Breadcrumb) 조회
                List<ProductDetailResponseDto.CategoryDto> categoryPath = new ArrayList<>();
                Long currentCatNo = product.getCategoryNo();
                while (currentCatNo != null) {
                        final Long searchNo = currentCatNo;
                        Category cat = categoryRepository.findById(searchNo).orElse(null);
                        if (cat != null) {
                                categoryPath.add(0, ProductDetailResponseDto.CategoryDto.builder()
                                                .id(cat.getCategoryNo())
                                                .name(cat.getName())
                                                .depth(cat.getDepth())
                                                .build());
                                currentCatNo = cat.getParentNo();
                        } else {
                                break;
                        }
                }

                // 6. 현재 최고 입찰자 여부 (memberNo 직접 비교 — 닉네임 비교보다 신뢰성 높음)
                boolean isHighestBidder = false;
                if (currentMemberNo != null) {
                        isHighestBidder = bidHistoryRepository
                                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(productNo, 0)
                                .map(topBid -> topBid.getMemberNo().equals(currentMemberNo))
                                .orElse(false);
                }

                // 7. 상세 데이터 조립 및 반환
                return ProductDetailResponseDto.builder()
                                .productNo(product.getProductNo())
                                .title(product.getTitle())
                                .description(product.getDescription())
                                .tradeType(product.getTradeType())
                                .location(product.getTradeAddrShort() != null ? product.getTradeAddrShort()
                                                : product.getTradeAddrDetail())
                                .startPrice(product.getStartPrice())
                                .currentPrice(product.getCurrentPrice())
                                .minBidUnit(product.getMinBidUnit())
                                .createdAt(product.getCreatedAt())
                                .endTime(product.getEndTime())
                                .buyoutPrice(product.getBuyoutPrice())
                                .shippingFee(product.getShippingFee())
                                .status(product.getStatus())
                                .participantCount(
                                                bidHistoryRepository.countDistinctParticipants(product.getProductNo()))
                                .images(imageUrls)
                                .categoryPath(categoryPath)
                                .isWishlisted(currentMemberNo != null
                                                ? wishlistRepository.existsByMemberNoAndProductNo(currentMemberNo,
                                                                product.getProductNo())
                                                : false)
                                .wishlistCount(wishlistRepository.countByProductNo(product.getProductNo()))
                                .seller(ProductDetailResponseDto.SellerInfoDto.builder()
                                                .sellerNo(seller.getMemberNo())
                                                .nickname(seller.getNickname())
                                                .mannerTemp(seller.getMannerTemp())
                                                .profileImgUrl(seller.getProfileImgUrl())
                                                .build())
                                .bidHistory(bidHistory)
                                .isHighestBidder(isHighestBidder)
                                .build();
        }

        @Override
        public ProductResponseDto findById(Long productNo) {
                Product product = productRepository.findById(productNo)
                                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));

                ProductImage mainImage = productImageRepository
                                .findFirstByProductNoAndIsMainOrderByImageNoAsc(productNo, 1);
                String imageUrl = mainImage != null ? "/api/images/" + mainImage.getUuidName() : null;

                return ProductResponseDto.builder()
                                .productNo(product.getProductNo())
                                .title(product.getTitle())
                                .currentPrice(product.getCurrentPrice())
                                .location(product.getTradeAddrShort() != null ? product.getTradeAddrShort()
                                                : product.getTradeAddrDetail())
                                .endTime(product.getEndTime())
                                .status(product.getStatus()) // [수정] isActive → status
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
                }).toList();
        }

        @Override
        @Transactional
        public void saveImages(Long productNo, List<MultipartFile> images) throws java.io.IOException {
                List<ProductImage> storedImages = fileStore.storeFiles(images, productNo);
                if (!storedImages.isEmpty()) {
                        productImageRepository.saveAll(storedImages);
                }
        }

        @Override
        public List<ProductListResponseDto> getMySellingProducts(Long memberNo) {
                List<Product> products = productRepository.findBySellerNoOrderByProductNoDesc(memberNo);
                List<ProductListResponseDto> dtos = toProductListDtos(products, memberNo);

                // 판매 완료 상품에 대해 낙찰 결과 상태(auctionResultStatus) 보강
                List<Long> completedProductNos = dtos.stream()
                        .filter(dto -> "completed".equals(dto.getStatus()))
                        .map(ProductListResponseDto::getId)
                        .toList();

                if (completedProductNos.isEmpty()) return dtos;

                // 낙찰 입찰 일괄 조회 → productNo : auctionResultStatus 맵 생성
                Map<Long, String> auctionStatusMap = new HashMap<>();
                for (Long productNo : completedProductNos) {
                        bidHistoryRepository
                                .findFirstByProductNoAndIsWinnerOrderByBidPriceDesc(productNo, 1)
                                .ifPresent(bid ->
                                        auctionResultRepository.findFirstByBidNo(bid.getBidNo())
                                                .ifPresent(result ->
                                                        auctionStatusMap.put(productNo, result.getStatus())));
                }

                if (auctionStatusMap.isEmpty()) return dtos;

                // DTO에 auctionResultStatus 주입
                return dtos.stream().map(dto -> {
                        String status = auctionStatusMap.get(dto.getId());
                        if (status == null) return dto;
                        return ProductListResponseDto.builder()
                                .id(dto.getId())
                                .title(dto.getTitle())
                                .location(dto.getLocation())
                                .currentPrice(dto.getCurrentPrice())
                                .endTime(dto.getEndTime())
                                .participantCount(dto.getParticipantCount())
                                .status(dto.getStatus())
                                .images(dto.getImages())
                                .isWishlisted(dto.isWishlisted())
                                .bidStatus(dto.getBidStatus())
                                .auctionResultStatus(status)
                                .build();
                }).toList();
        }

        @Override
        public List<ProductListResponseDto> getMyBiddingProducts(Long memberNo) {
                List<Long> productNos = bidHistoryRepository.findDistinctProductNosByMemberNo(memberNo);
                if (productNos.isEmpty())
                        return List.of();

                // findAllById는 순서를 보장하지 않으므로, productNos 순서대로 재정렬 필요
                List<Product> fetchedProducts = productRepository.findAllById(productNos);
                Map<Long, Product> productMap = fetchedProducts.stream()
                                .collect(Collectors.toMap(Product::getProductNo, Function.identity()));
                List<Product> products = productNos.stream()
                                .map(productMap::get)
                                .filter(Objects::nonNull)
                                .toList();

                // 낙찰 여부 배치 조회
                Set<Long> wonProductNos = new java.util.HashSet<>(
                                bidHistoryRepository.findWonProductNosInList(memberNo, productNos));

                // 결제완료 또는 구매확정된 낙찰 상품은 구매내역으로 이동 → 입찰내역에서 제외
                Map<Long, String> auctionStatusMap = new HashMap<>();
                if (!wonProductNos.isEmpty()) {
                        List<BidHistory> winnerBids = wonProductNos.stream()
                                        .map(pNo -> bidHistoryRepository
                                                        .findFirstByProductNoAndIsWinnerOrderByBidPriceDesc(pNo, 1)
                                                        .orElse(null))
                                        .filter(b -> b != null)
                                        .toList();
                        List<Long> winnerBidNos = winnerBids.stream().map(BidHistory::getBidNo).toList();
                        if (!winnerBidNos.isEmpty()) {
                                List<AuctionResult> results = auctionResultRepository.findByBidNos(winnerBidNos);
                                
                                // "구매확정"된 상품만 입찰내역에서 완전히 제외 (결제완료는 남아있음)
                                Set<Long> confirmedBidNos = results.stream()
                                                .filter(ar -> "구매확정".equals(ar.getStatus()))
                                                .map(AuctionResult::getBidNo)
                                                .collect(Collectors.toSet());
                                
                                Set<Long> confirmedProductNos = winnerBids.stream()
                                                .filter(b -> confirmedBidNos.contains(b.getBidNo()))
                                                .map(BidHistory::getProductNo)
                                                .collect(Collectors.toSet());
                                
                                products = products.stream()
                                                .filter(p -> !confirmedProductNos.contains(p.getProductNo()))
                                                .toList();
                                wonProductNos.removeAll(confirmedProductNos);

                                // 각 상품별 낙찰 결과 상태 맵 생성
                                Map<Long, String> bidNoToStatus = results.stream()
                                                .collect(Collectors.toMap(AuctionResult::getBidNo, AuctionResult::getStatus, (s1, s2) -> s1));
                                for (BidHistory bh : winnerBids) {
                                    auctionStatusMap.put(bh.getProductNo(), bidNoToStatus.getOrDefault(bh.getBidNo(), "결제대기"));
                                }
                        }
                }

                // 현재 최고입찰자 배치 조회 (active 경매에서 내가 최고입찰자인지 판별)
                List<Long> activeProductNos = products.stream()
                                .filter(p -> !p.getEndTime().isBefore(java.time.LocalDateTime.now())
                                                && p.getStatus() == 0)
                                .map(Product::getProductNo)
                                .toList();
                java.util.Map<Long, Long> topBidderMap = new java.util.HashMap<>();
                if (!activeProductNos.isEmpty()) {
                        bidHistoryRepository.findTopBidderByProductNos(activeProductNos)
                                        .forEach(row -> topBidderMap.putIfAbsent((Long) row[0], (Long) row[1]));
                }

                return toProductListDtosWithBidStatus(products, memberNo, wonProductNos, topBidderMap, auctionStatusMap);
        }

        @Override
        public List<ProductListResponseDto> getMyPurchasedProducts(Long memberNo) {
                // 1. 내가 낙찰받은 상품 번호 목록
                List<Long> wonProductNos = bidHistoryRepository.findWonProductNosByMemberNo(memberNo);
                if (wonProductNos.isEmpty())
                        return List.of();

                // 2. 해당 상품들의 낙찰 입찰번호를 찾고, AuctionResult에서 구매확정된 것만 필터
                List<Product> fetchedProducts = productRepository.findAllById(wonProductNos);
                Map<Long, Product> productMap = fetchedProducts.stream()
                                .collect(Collectors.toMap(Product::getProductNo, Function.identity()));
                List<Product> products = wonProductNos.stream()
                                .map(productMap::get)
                                .filter(Objects::nonNull)
                                .toList();

                // 낙찰된 bid 조회
                List<BidHistory> winnerBids = wonProductNos.stream()
                                .map(pNo -> bidHistoryRepository
                                                .findFirstByProductNoAndIsWinnerOrderByBidPriceDesc(pNo, 1)
                                                .orElse(null))
                                .filter(b -> b != null)
                                .toList();

                List<Long> bidNos = winnerBids.stream().map(BidHistory::getBidNo).toList();
                if (bidNos.isEmpty())
                        return List.of();

                // AuctionResult 중 결제완료 또는 구매확정된 것 필터 (결제 완료 시 구매내역으로 이동)
                List<AuctionResult> results = auctionResultRepository.findByBidNos(bidNos);
                Set<Long> confirmedBidNos = results.stream()
                                .filter(ar -> "구매확정".equals(ar.getStatus()))
                                .map(AuctionResult::getBidNo)
                                .collect(Collectors.toSet());

                // 구매확정된 bid의 productNo만 추출
                Set<Long> confirmedProductNos = winnerBids.stream()
                                .filter(b -> confirmedBidNos.contains(b.getBidNo()))
                                .map(BidHistory::getProductNo)
                                .collect(Collectors.toSet());

                List<Product> confirmedProducts = products.stream()
                                .filter(p -> confirmedProductNos.contains(p.getProductNo()))
                                .toList();

                return toProductListDtos(confirmedProducts, memberNo);
        }

        @Override
        public List<ProductListResponseDto> getMyWishlistProducts(Long memberNo) {
                List<Long> productNos = wishlistRepository.findProductNosByMemberNo(memberNo);
                if (productNos.isEmpty())
                        return List.of();

                List<Product> fetchedProducts = productRepository.findAllById(productNos);
                Map<Long, Product> productMap = fetchedProducts.stream()
                                .collect(Collectors.toMap(Product::getProductNo, Function.identity()));
                List<Product> products = productNos.stream()
                                .map(productMap::get)
                                .filter(Objects::nonNull)
                                .toList();

                return toProductListDtos(products, memberNo);
        }

        @Override
        @Transactional
        public void deleteProduct(Long productNo, Long memberNo) {
                Product product = productRepository.findById(productNo)
                                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));
                if (!product.getSellerNo().equals(memberNo)) {
                        throw new IllegalStateException("본인의 상품만 삭제할 수 있습니다.");
                }
                product.setIsDeleted(1);
                product.setStatus(2);
                auctionExpiryWatchdog.cancel(productNo);
        }

        /**
         * Product 엔티티 리스트를 ProductListResponseDto 리스트로 변환 (입찰 상태 포함)
         */
        private List<ProductListResponseDto> toProductListDtosWithBidStatus(
                        List<Product> products, Long memberNo, Set<Long> wonProductNos,
                        java.util.Map<Long, Long> topBidderMap,
                        java.util.Map<Long, String> auctionStatusMap) {
                products = products.stream()
                                .filter(p -> p.getIsDeleted() == 0)
                                .toList();
                if (products.isEmpty())
                        return List.of();

                List<Long> productNos = products.stream().map(Product::getProductNo).toList();

                Map<Long, ProductImage> mainImageMap = productImageRepository.findMainImagesByProductNos(productNos)
                                .stream()
                                .collect(Collectors.toMap(ProductImage::getProductNo, Function.identity(),
                                                (a, b) -> a));

                Set<Long> wishlistedNos = (memberNo != null)
                                ? Set.copyOf(wishlistRepository.findWishlistedProductNos(memberNo, productNos))
                                : Set.of();

                Map<Long, Long> participantCountMap = bidHistoryRepository
                                .countDistinctParticipantsByProductNos(productNos).stream()
                                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

                return products.stream().map(product -> {
                        ProductImage mainImg = mainImageMap.get(product.getProductNo());
                        List<String> imageUrls = mainImg != null
                                        ? List.of("/api/images/" + mainImg.getUuidName())
                                        : List.of();

                        boolean isFinished = product.getEndTime().isBefore(LocalDateTime.now())
                                        || product.getStatus() != 0;

                        // 입찰 상태 결정: 상위입찰자 / 추월당함 / 낙찰 / 낙찰실패
                        String bidStatus;
                        if (!isFinished) {
                                Long topBidder = topBidderMap.get(product.getProductNo());
                                bidStatus = (topBidder != null && topBidder.equals(memberNo)) ? "bidding" : "outbid";
                        } else if (wonProductNos.contains(product.getProductNo())) {
                                bidStatus = "won"; // 낙찰
                        } else {
                                bidStatus = "lost"; // 낙찰실패
                        }

                        return ProductListResponseDto.builder()
                                        .id(product.getProductNo())
                                        .title(product.getTitle())
                                        .location(product.getTradeAddrShort() != null ? product.getTradeAddrShort()
                                                        : product.getTradeAddrDetail())
                                        .currentPrice(product.getCurrentPrice())
                                        .endTime(product.getEndTime())
                                        .participantCount(participantCountMap.getOrDefault(product.getProductNo(), 0L))
                                        .status(isFinished ? "completed" : "active")
                                        .images(imageUrls)
                                        .isWishlisted(wishlistedNos.contains(product.getProductNo()))
                                        .bidStatus(bidStatus)
                                        .auctionResultStatus(auctionStatusMap != null ? auctionStatusMap.get(product.getProductNo()) : null)
                                        .build();
                }).toList();
        }

        @Override
        public List<AdminProductResponseDto> getAllProductsForAdmin() {
                List<Product> products = productRepository.findByIsDeletedOrderByCreatedAtDesc(0);

                List<Long> productNos = products.stream().map(Product::getProductNo).toList();
                if (productNos.isEmpty())
                        return List.of();

                // 판매자 닉네임 배치 조회
                List<Long> sellerNos = products.stream().map(Product::getSellerNo).distinct().toList();
                Map<Long, String> sellerNicknameMap = memberRepository.findAllById(sellerNos).stream()
                                .collect(Collectors.toMap(Member::getMemberNo, Member::getNickname));

                // 메인 이미지 배치 조회
                Map<Long, ProductImage> mainImageMap = productImageRepository.findMainImagesByProductNos(productNos)
                                .stream()
                                .collect(Collectors.toMap(ProductImage::getProductNo, Function.identity(),
                                                (a, b) -> a));

                // 참여자 수 배치 조회
                Map<Long, Long> participantCountMap = bidHistoryRepository
                                .countDistinctParticipantsByProductNos(productNos).stream()
                                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

                return products.stream().map(product -> {
                        ProductImage mainImg = mainImageMap.get(product.getProductNo());
                        String imageUrl = mainImg != null ? "/api/images/" + mainImg.getUuidName() : null;

                        return AdminProductResponseDto.builder()
                                        .productNo(product.getProductNo())
                                        .title(product.getTitle())
                                        .mainImageUrl(imageUrl)
                                        .sellerNickname(sellerNicknameMap.getOrDefault(product.getSellerNo(), "알 수 없음"))
                                        .sellerNo(product.getSellerNo())
                                        .startPrice(product.getStartPrice())
                                        .currentPrice(product.getCurrentPrice())
                                        .participantCount(participantCountMap.getOrDefault(product.getProductNo(), 0L))
                                        .endTime(product.getEndTime())
                                        .status(product.getStatus())
                                        .build();
                }).toList();
        }

        @Override
        @Transactional
        public void cancelAuctionByAdmin(Long productNo) {
                Product product = productRepository.findById(productNo)
                                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));
                if (product.getStatus() != 0) {
                        throw new IllegalStateException("진행 중인 경매만 강제 종료할 수 있습니다.");
                }
                product.setStatus(2);
                auctionExpiryWatchdog.cancel(productNo);

                // 입찰자 전원에게 경매 취소 알림
                try {
                        List<Long> bidderNos = bidHistoryRepository.findDistinctBiddersByProductNo(productNo);
                        for (Long bidderNo : bidderNos) {
                                notificationService.sendAndSaveNotification(
                                                bidderNo, "bid",
                                                "[" + product.getTitle() + "] 경매가 판매자 사정으로 취소되었습니다.",
                                                "/");
                        }
                } catch (Exception e) {
                        log.warn("[ProductService] 경매 취소 알림 전송 실패: {}", e.getMessage());
                }
        }

        /**
         * 판매자 경매 취소 (3단계 조건)
         *
         * <ul>
         *   <li>조건 A: 입찰자 0명 → 즉시 취소, 패널티 없음</li>
         *   <li>조건 B: 입찰자 ≥ 1명 && 마감 12시간 초과 → 취소 가능, 패널티 부과 (매너온도 -10, 포인트 벌금)</li>
         *   <li>조건 C: 입찰자 ≥ 1명 && 마감 12시간 이내 → 취소 불가, 관리자 신고 유도</li>
         * </ul>
         */
        @Override
        @Transactional
        public void cancelAuctionBySeller(Long productNo, Long memberNo) {
                // 비관적 락으로 동시 취소/입찰 방지
                Product product = productRepository.findByIdWithLock(productNo)
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

                if (!product.getSellerNo().equals(memberNo)) {
                        throw new IllegalStateException("본인의 상품만 취소할 수 있습니다.");
                }
                if (product.getStatus() != 0) {
                        throw new IllegalStateException("진행 중인 경매만 취소할 수 있습니다.");
                }

                long bidderCount = bidHistoryRepository.countDistinctParticipants(productNo);
                long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), product.getEndTime());

                // ── 조건 C: 마감 임박 + 입찰자 존재 → 취소 불가 ──────────────────────
                if (bidderCount > 0 && hoursLeft < 12) {
                        throw new IllegalStateException(
                                "경매 마감 12시간 이내에는 임의 취소가 불가합니다. " +
                                "물건 파손 등 불가피한 사유가 있다면 관리자에게 취소를 신청해 주세요.");
                }

                // ── 조건 B: 입찰자 존재 && 마감 12시간 초과 → 패널티 + 환불 ──────────
                if (bidderCount > 0) {
                        Member seller = memberRepository.findByIdWithLock(memberNo)
                                        .orElseThrow(() -> new IllegalStateException("판매자 정보를 찾을 수 없습니다."));

                        // 매너온도 패널티: -10점 (최저 0점)
                        double prevTemp = seller.getMannerTemp();
                        double newTemp = Math.max(0.0, prevTemp - 10.0);
                        seller.setMannerTemp(newTemp);
                        mannerHistoryRepository.save(MannerHistory.builder()
                                        .memberNo(memberNo)
                                        .previousTemp(prevTemp)
                                        .newTemp(newTemp)
                                        .reason("[" + product.getTitle() + "] 경매 임의 취소 패널티")
                                        .build());

                        // 포인트 벌금: max(1,000, currentPrice × 3%), 잔액 부족 시 취소 불가
                        long penalty = Math.max(1_000L, (long) (product.getCurrentPrice() * 0.03));
                        if (seller.getPoints() < penalty) {
                                throw new IllegalStateException(
                                        "포인트가 부족하여 취소할 수 없습니다. " +
                                        "(필요 패널티: " + penalty + "원, 보유: " + seller.getPoints() + "원)");
                        }
                        seller.setPoints(seller.getPoints() - penalty);
                        pointHistoryRepository.save(PointHistory.builder()
                                        .memberNo(memberNo)
                                        .type("취소패널티")
                                        .amount(-penalty)
                                        .balance(seller.getPoints())
                                        .reason("[" + product.getTitle() + "] 경매 취소 패널티")
                                        .build());
                        try { sseService.sendPointUpdate(memberNo, seller.getPoints()); }
                        catch (Exception e) { log.warn("[AuctionCancel] 판매자 포인트 SSE 실패: {}", e.getMessage()); }

                        // 현재 최고 입찰자 포인트 환불 (이전 입찰자들은 outbid 시 이미 환불됨)
                        Optional<BidHistory> topBidOpt = bidHistoryRepository
                                        .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(productNo, 0);
                        if (topBidOpt.isPresent()) {
                                BidHistory topBid = topBidOpt.get();
                                Member topBidder = memberRepository.findByIdWithLock(topBid.getMemberNo())
                                                .orElse(null);
                                if (topBidder != null) {
                                        topBidder.setPoints(topBidder.getPoints() + topBid.getBidPrice());
                                        pointHistoryRepository.save(PointHistory.builder()
                                                        .memberNo(topBidder.getMemberNo())
                                                        .type("입찰환불")
                                                        .amount(topBid.getBidPrice())
                                                        .balance(topBidder.getPoints())
                                                        .reason("[" + product.getTitle() + "] 판매자 경매 취소로 인한 환불")
                                                        .build());
                                        try { sseService.sendPointUpdate(topBidder.getMemberNo(), topBidder.getPoints()); }
                                        catch (Exception e) { log.warn("[AuctionCancel] 입찰자 포인트 SSE 실패: {}", e.getMessage()); }
                                }
                        }
                }

                // ── 공통: 활성 자동입찰 전원 비활성화 ──────────────────────────────────
                List<AutoBid> activeAutoBids = autoBidRepository.findActiveByProductNo(productNo);
                for (AutoBid autoBid : activeAutoBids) {
                        autoBid.setIsActive(0);
                        autoBid.setUpdatedAt(LocalDateTime.now());
                        autoBidRepository.save(autoBid);
                }

                // ── 공통: 상품 상태를 취소(2)로 변경 + 스케줄러 취소 ────────────────────
                product.setStatus(2);
                auctionExpiryWatchdog.cancel(productNo);

                // ── 공통: 입찰자 전원에게 알림 발송 (취소 알림) ─────────────────────────
                List<Long> bidderNos = bidHistoryRepository.findDistinctBiddersByProductNo(productNo);
                for (Long bidderNo : bidderNos) {
                        try {
                                notificationService.sendAndSaveNotification(
                                                bidderNo, "bid",
                                                "[" + product.getTitle() + "] 판매자의 사정으로 경매가 취소되었습니다.",
                                                "/products/" + productNo);
                        } catch (Exception e) {
                                log.warn("[AuctionCancel] 알림 전송 실패 (bidderNo={}): {}", bidderNo, e.getMessage());
                        }
                }

                // ── 공통: SSE 취소 이벤트 브로드캐스트 ─────────────────────────────────
                try { sseService.broadcastAuctionCancelled(productNo); }
                catch (Exception e) { log.warn("[AuctionCancel] SSE 브로드캐스트 실패: {}", e.getMessage()); }

                log.info("[AuctionCancel] 판매자 취소 완료: productNo={}, memberNo={}, bidders={}",
                        productNo, memberNo, bidderNos.size());
        }

        @Override
        public List<Map<String, Object>> getCategoryStats() {
                List<Object[]> rows = productRepository.countProductsByRootCategory();
                return rows.stream().map(row -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", row[0]);
                        map.put("count", row[1]);
                        return map;
                }).toList();
        }

        /**
         * Product 엔티티 리스트를 ProductListResponseDto 리스트로 변환 (배치 쿼리 사용)
         */
        private List<ProductListResponseDto> toProductListDtos(List<Product> products, Long memberNo) {
                // 삭제된 상품 제외
                products = products.stream()
                                .filter(p -> p.getIsDeleted() == 0)
                                .toList();

                if (products.isEmpty())
                        return List.of();

                List<Long> productNos = products.stream().map(Product::getProductNo).toList();

                // 메인 이미지 배치 조회
                Map<Long, ProductImage> mainImageMap = productImageRepository.findMainImagesByProductNos(productNos)
                                .stream()
                                .collect(Collectors.toMap(ProductImage::getProductNo, Function.identity(),
                                                (a, b) -> a));

                // 찜 여부 배치 조회
                Set<Long> wishlistedNos = (memberNo != null)
                                ? Set.copyOf(wishlistRepository.findWishlistedProductNos(memberNo, productNos))
                                : Set.of();

                // 참여자 수 배치 조회
                Map<Long, Long> participantCountMap = bidHistoryRepository
                                .countDistinctParticipantsByProductNos(productNos).stream()
                                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

                return products.stream().map(product -> {
                        ProductImage mainImg = mainImageMap.get(product.getProductNo());
                        List<String> imageUrls = mainImg != null
                                        ? List.of("/api/images/" + mainImg.getUuidName())
                                        : List.of();

                        boolean isFinished = product.getEndTime().isBefore(LocalDateTime.now())
                                        || product.getStatus() != 0;

                        return ProductListResponseDto.builder()
                                        .id(product.getProductNo())
                                        .title(product.getTitle())
                                        .location(product.getTradeAddrShort() != null ? product.getTradeAddrShort()
                                                        : product.getTradeAddrDetail())
                                        .currentPrice(product.getCurrentPrice())
                                        .endTime(product.getEndTime())
                                        .participantCount(participantCountMap.getOrDefault(product.getProductNo(), 0L))
                                        .status(isFinished ? "completed" : "active")
                                        .images(imageUrls)
                                        .isWishlisted(wishlistedNos.contains(product.getProductNo()))
                                        .build();
                }).toList();
        }
}