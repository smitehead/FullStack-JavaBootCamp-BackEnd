package com.javajava.project.domain.community.service;

import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.community.dto.ReviewRequestDto;
import com.javajava.project.domain.community.dto.ReviewResponseDto;
import com.javajava.project.domain.community.entity.Review;
import com.javajava.project.domain.community.repository.ReviewRepository;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    /**
     * 리뷰 작성 + 매너온도 자동 반영
     */
    @Transactional
    public ReviewResponseDto createReview(Long writerNo, ReviewRequestDto dto) {
        // 1. 낙찰 결과 확인
        AuctionResult result = auctionResultRepository.findById(dto.getResultNo())
                .orElseThrow(() -> new IllegalArgumentException("낙찰 결과를 찾을 수 없습니다."));

        if (!"구매확정".equals(result.getStatus())) {
            throw new IllegalStateException("구매 확정된 거래만 리뷰를 작성할 수 있습니다.");
        }

        // 2. 중복 리뷰 방지
        if (reviewRepository.findByResultNo(dto.getResultNo()).isPresent()) {
            throw new IllegalStateException("이미 리뷰를 작성한 거래입니다.");
        }

        // 3. 낙찰자(구매자) 본인 확인
        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));

        if (!bid.getMemberNo().equals(writerNo)) {
            throw new IllegalStateException("낙찰자 본인만 리뷰를 작성할 수 있습니다.");
        }

        // 4. 리뷰 대상 = 판매자
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));
        Long targetNo = product.getSellerNo();

        // 5. 태그 → 콤마 구분 문자열 변환
        String tagsStr = (dto.getTags() != null && !dto.getTags().isEmpty())
                ? String.join(",", dto.getTags()) : null;

        // 6. 리뷰 저장
        Review review = Review.builder()
                .resultNo(dto.getResultNo())
                .writerNo(writerNo)
                .targetNo(targetNo)
                .rating(dto.getRating())
                .tags(tagsStr)
                .content(dto.getContent())
                .isHidden(0)
                .build();
        reviewRepository.save(review);

        // 7. 매너온도 자동 계산 (별점이 있는 경우만)
        if (dto.getRating() != null) {
            updateMannerTemp(targetNo);
        }

        // 8. 판매자에게 알림
        Member writer = memberRepository.findById(writerNo)
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보를 찾을 수 없습니다."));
        try {
            notificationService.sendAndSaveNotification(
                    targetNo, "activity",
                    writer.getNickname() + "님이 [" + product.getTitle() + "] 거래에 리뷰를 남겼습니다.",
                    "/products/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[ReviewService] 리뷰 알림 전송 실패: {}", e.getMessage());
        }

        return ReviewResponseDto.from(review, writer.getNickname());
    }

    /**
     * 특정 회원이 받은 리뷰 목록 (프로필용)
     */
    public List<ReviewResponseDto> getReviewsByTarget(Long targetNo) {
        return reviewRepository.findByTargetNoAndIsHidden(targetNo, 0).stream()
                .map(review -> {
                    String nickname = memberRepository.findById(review.getWriterNo())
                            .map(Member::getNickname).orElse("탈퇴회원");
                    return ReviewResponseDto.from(review, nickname);
                }).toList();
    }

    /**
     * 내가 작성한 리뷰 목록 (마이페이지용)
     */
    public List<ReviewResponseDto> getMyReviews(Long writerNo) {
        Member writer = memberRepository.findById(writerNo)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        return reviewRepository.findByWriterNo(writerNo).stream()
                .map(review -> ReviewResponseDto.from(review, writer.getNickname()))
                .toList();
    }

    /**
     * 매너온도 자동 계산
     * 공식: 36.5 + (평균별점 - 3.0) * 리뷰수 보정
     * - 별점 3점 = 기본 온도 유지
     * - 별점 5점 = 온도 상승, 별점 1점 = 온도 하락
     * - 리뷰가 많을수록 반영 비중 증가 (최대 +-10도)
     */
    private void updateMannerTemp(Long targetNo) {
        Double avgRating = reviewRepository.findAverageRatingByTargetNo(targetNo);
        if (avgRating == null) return;

        long reviewCount = reviewRepository.findByTargetNoAndIsHidden(targetNo, 0).size();

        // 보정 계수: 리뷰 수에 따라 0.5 ~ 5.0 범위 (리뷰 10개 이상이면 최대)
        double weight = Math.min(reviewCount, 10) * 0.5;
        double delta = (avgRating - 3.0) * weight;

        // 매너온도 = 기본(36.5) + 변동분, 범위: 0 ~ 100
        double newTemp = Math.max(0, Math.min(100, 36.5 + delta));

        Member target = memberRepository.findById(targetNo)
                .orElseThrow(() -> new IllegalArgumentException("대상 회원 정보를 찾을 수 없습니다."));
        target.setMannerTemp(newTemp);
    }
}
