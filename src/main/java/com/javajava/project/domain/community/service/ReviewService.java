package com.javajava.project.domain.community.service;

import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.auction.entity.AuctionResultStatus;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.community.dto.ReviewRequestDto;
import com.javajava.project.domain.community.dto.ReviewResponseDto;
import com.javajava.project.domain.community.dto.ReviewTagDefResponseDto;
import com.javajava.project.domain.community.entity.Review;
import com.javajava.project.domain.community.entity.ReviewTag;
import com.javajava.project.domain.community.repository.ReviewRepository;
import com.javajava.project.domain.community.repository.ReviewTagDefRepository;
import com.javajava.project.domain.community.repository.ReviewTagRepository;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.member.entity.MannerHistory;
import com.javajava.project.domain.member.repository.MannerHistoryRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final Set<String> NEGATIVE_TAG_NAMES = Set.of(
            "응답이 느렸어요", "불친절했어요", "약속을 지키지 않았어요",
            "상품 상태가 설명과 달랐어요", "결제가 늦었어요", "연락이 되지 않았어요"
    );

    private final ReviewRepository reviewRepository;
    private final ReviewTagDefRepository reviewTagDefRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final MemberRepository memberRepository;
    private final MannerHistoryRepository mannerHistoryRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    /**
     * 리뷰 작성 + 태그 저장
     */
    @Transactional
    public ReviewResponseDto createReview(Long writerNo, ReviewRequestDto dto) {
        AuctionResult result = auctionResultRepository.findById(dto.getResultNo())
                .orElseThrow(() -> new IllegalArgumentException("낙찰 결과를 찾을 수 없습니다."));

        if (!AuctionResultStatus.PURCHASE_CONFIRMED.equals(result.getStatus())) {
            throw new IllegalStateException("구매 확정된 거래만 리뷰를 작성할 수 있습니다.");
        }

        if (reviewRepository.existsByResultNoAndWriterNo(dto.getResultNo(), writerNo)) {
            throw new IllegalStateException("이미 작성된 후기입니다.");
        }

        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));

        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        Long buyerNo  = bid.getMemberNo();
        Long sellerNo = product.getSellerNo();

        if (buyerNo.equals(sellerNo)) {
            throw new IllegalStateException("구매자와 판매자가 동일한 거래는 후기를 작성할 수 없습니다.");
        }

        Long targetNo;
        String writerRole;
        if (writerNo.equals(buyerNo)) {
            targetNo   = sellerNo;
            writerRole = "BUYER";
        } else if (writerNo.equals(sellerNo)) {
            targetNo   = buyerNo;
            writerRole = "SELLER";
        } else {
            throw new IllegalStateException("해당 거래의 구매자 또는 판매자만 후기를 작성할 수 있습니다.");
        }

        Review review = Review.builder()
                .resultNo(dto.getResultNo())
                .writerNo(writerNo)
                .targetNo(targetNo)
                .writerRole(writerRole)
                .content(dto.getContent())
                .isHidden(0)
                .build();
        reviewRepository.save(review);

        // 선택된 태그 ID → REVIEW_TAG 저장 + 매너온도 반영
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<ReviewTag> tags = dto.getTagIds().stream()
                    .map(tagId -> ReviewTag.builder()
                            .reviewNo(review.getReviewNo())
                            .tagId(tagId)
                            .build())
                    .toList();
            reviewTagRepository.saveAll(tags);

            List<String> selectedTagNames = reviewTagDefRepository.findAllById(dto.getTagIds()).stream()
                    .map(com.javajava.project.domain.community.entity.ReviewTagDef::getTagName)
                    .toList();
            long negativeCount = selectedTagNames.stream().filter(NEGATIVE_TAG_NAMES::contains).count();
            long positiveCount = selectedTagNames.size() - negativeCount;

            double delta = Math.min(positiveCount * 0.1, 0.3) - Math.min(negativeCount * 0.2, 0.5);
            if (delta != 0) {
                Member target = memberRepository.findById(targetNo)
                        .orElseThrow(() -> new IllegalArgumentException("대상 회원을 찾을 수 없습니다."));
                double prevTemp = target.getMannerTemp();
                target.setMannerTemp(prevTemp + delta);
                mannerHistoryRepository.save(MannerHistory.builder()
                        .memberNo(targetNo)
                        .previousTemp(prevTemp)
                        .newTemp(target.getMannerTemp())
                        .reason("[" + product.getTitle() + "] 거래 후기 평가 (" + (delta > 0 ? "+" : "") + String.format("%.1f", delta) + ")")
                        .build());
            }
        }

        Member writer = memberRepository.findById(writerNo)
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보를 찾을 수 없습니다."));
        try {
            notificationService.sendAndSaveNotification(
                    targetNo, "activity",
                    writer.getNickname() + "님이 [" + product.getTitle() + "] 거래에 리뷰를 남겼습니다.",
                    "/mypage?tab=reviews");
        } catch (Exception e) {
            log.warn("[ReviewService] 리뷰 알림 전송 실패: {}", e.getMessage());
        }

        List<String> tagNames = reviewTagDefRepository.findTagNamesByReviewNo(review.getReviewNo());
        return ReviewResponseDto.from(review, writer.getNickname(), product.getProductNo(), product.getTitle(), tagNames);
    }

    /**
     * 특정 회원이 받은 리뷰 목록 (프로필용)
     */
    public List<ReviewResponseDto> getReviewsByTarget(Long targetNo) {
        List<Review> reviews = reviewRepository.findByTargetNoAndIsHidden(targetNo, 0);
        if (reviews.isEmpty()) return List.of();
        return buildReviewDtos(reviews, null);
    }

    /**
     * 내가 작성한 리뷰 목록 (마이페이지용)
     */
    public List<ReviewResponseDto> getMyReviews(Long writerNo) {
        Member writer = memberRepository.findById(writerNo)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        List<Review> reviews = reviewRepository.findByWriterNoOrderByCreatedAtDesc(writerNo);
        if (reviews.isEmpty()) return List.of();
        // writerNo가 고정이므로 nicknameMap 대신 writer 닉네임을 직접 전달
        return buildReviewDtos(reviews, writer.getNickname());
    }

    /**
     * 리뷰 목록 → DTO 변환 (배치 조회).
     * fixedNickname이 null이면 리뷰별 writerNo로 닉네임을 배치 조회한다 (getReviewsByTarget용).
     * fixedNickname이 non-null이면 모든 리뷰에 동일 닉네임을 사용한다 (getMyReviews용).
     */
    private List<ReviewResponseDto> buildReviewDtos(List<Review> reviews, String fixedNickname) {
        // 1. 작성자 닉네임 배치 조회 (fixedNickname이 없는 경우만)
        Map<Long, String> nicknameMap = Map.of();
        if (fixedNickname == null) {
            Set<Long> writerNos = reviews.stream().map(Review::getWriterNo).collect(Collectors.toSet());
            nicknameMap = memberRepository.findAllById(writerNos).stream()
                    .collect(Collectors.toMap(Member::getMemberNo, Member::getNickname));
        }

        // 2. AuctionResult → BidHistory → Product 배치 조회
        Set<Long> resultNos = reviews.stream().map(Review::getResultNo).collect(Collectors.toSet());
        Map<Long, AuctionResult> resultMap = auctionResultRepository.findAllById(resultNos).stream()
                .collect(Collectors.toMap(AuctionResult::getResultNo, r -> r));

        Set<Long> bidNos = resultMap.values().stream().map(AuctionResult::getBidNo).collect(Collectors.toSet());
        Map<Long, BidHistory> bidMap = bidHistoryRepository.findAllById(bidNos).stream()
                .collect(Collectors.toMap(BidHistory::getBidNo, b -> b));

        Set<Long> productNos = bidMap.values().stream().map(BidHistory::getProductNo).collect(Collectors.toSet());
        Map<Long, Product> productMap = productRepository.findAllById(productNos).stream()
                .collect(Collectors.toMap(Product::getProductNo, p -> p));

        // 3. 태그명 배치 조회
        List<Long> reviewNos = reviews.stream().map(Review::getReviewNo).toList();
        Map<Long, List<String>> tagNamesMap = reviewTagDefRepository.findTagNamesByReviewNos(reviewNos).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));

        final Map<Long, String> finalNicknameMap = nicknameMap;
        return reviews.stream().map(review -> {
            String nickname = fixedNickname != null
                    ? fixedNickname
                    : finalNicknameMap.getOrDefault(review.getWriterNo(), "탈퇴회원");
            AuctionResult result = resultMap.get(review.getResultNo());
            BidHistory bid = result != null ? bidMap.get(result.getBidNo()) : null;
            Product product = bid != null ? productMap.get(bid.getProductNo()) : null;
            List<String> tagNames = tagNamesMap.getOrDefault(review.getReviewNo(), List.of());
            return ReviewResponseDto.from(review, nickname,
                    product != null ? product.getProductNo() : null,
                    product != null ? product.getTitle() : null,
                    tagNames);
        }).toList();
    }

    /**
     * 작성자가 해당 거래에 이미 후기를 작성했는지 확인
     */
    public boolean existsByResultNoAndWriterNo(Long resultNo, Long writerNo) {
        return reviewRepository.existsByResultNoAndWriterNo(resultNo, writerNo);
    }

    /**
     * 현재 사용자의 해당 거래 역할 반환 (BUYER / SELLER)
     */
    public String getWriterRole(Long memberNo, Long resultNo) {
        AuctionResult result = auctionResultRepository.findById(resultNo)
                .orElseThrow(() -> new IllegalArgumentException("낙찰 결과를 찾을 수 없습니다."));
        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        if (memberNo.equals(bid.getMemberNo())) return "BUYER";
        if (memberNo.equals(product.getSellerNo())) return "SELLER";
        throw new IllegalStateException("해당 거래의 참여자가 아닙니다.");
    }

    /**
     * 역할별 사용 가능한 태그 목록 반환
     * role = "BUYER" → BUYER + ALL 태그
     * role = "SELLER" → SELLER + ALL 태그
     * role 미입력 → 전체 태그
     */
    public List<ReviewTagDefResponseDto> getAvailableTags(String role) {
        if (role == null || role.isBlank()) {
            return reviewTagDefRepository.findAll().stream()
                    .map(ReviewTagDefResponseDto::from).toList();
        }
        return reviewTagDefRepository.findByApplicableRoleIn(List.of(role.toUpperCase(), "ALL"))
                .stream().map(ReviewTagDefResponseDto::from).toList();
    }

    /**
     * 리뷰 숨김 처리 (수신자 본인만 가능)
     */
    @Transactional
    public void hideReview(Long memberNo, Long reviewNo) {
        Review review = reviewRepository.findById(reviewNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

        if (!review.getTargetNo().equals(memberNo)) {
            throw new IllegalStateException("본인이 받은 후기만 숨길 수 있습니다.");
        }

        review.setIsHidden(1);
    }
}
