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
                .tags(tagsStr)
                .content(dto.getContent())
                .isHidden(0)
                .build();
        reviewRepository.save(review);

        // 7. 판매자에게 알림
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

        return ReviewResponseDto.from(review, writer.getNickname(), product.getProductNo(), product.getTitle());
    }

    /**
     * 특정 회원이 받은 리뷰 목록 (프로필용)
     */
    public List<ReviewResponseDto> getReviewsByTarget(Long targetNo) {
        return reviewRepository.findByTargetNoAndIsHidden(targetNo, 0).stream()
                .map(review -> {
                    String nickname = memberRepository.findById(review.getWriterNo())
                            .map(Member::getNickname).orElse("탈퇴회원");
                    Long productNo = null;
                    String productTitle = null;
                    try {
                        AuctionResult result = auctionResultRepository.findById(review.getResultNo()).orElse(null);
                        if (result != null) {
                            BidHistory bid = bidHistoryRepository.findById(result.getBidNo()).orElse(null);
                            if (bid != null) {
                                Product product = productRepository.findById(bid.getProductNo()).orElse(null);
                                if (product != null) {
                                    productNo = product.getProductNo();
                                    productTitle = product.getTitle();
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    return ReviewResponseDto.from(review, nickname, productNo, productTitle);
                }).toList();
    }

    /**
     * 내가 작성한 리뷰 목록 (마이페이지용)
     */
    public List<ReviewResponseDto> getMyReviews(Long writerNo) {
        Member writer = memberRepository.findById(writerNo)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        return reviewRepository.findByWriterNo(writerNo).stream()
                .map(review -> {
                    Long productNo = null;
                    String productTitle = null;
                    try {
                        AuctionResult result = auctionResultRepository.findById(review.getResultNo()).orElse(null);
                        if (result != null) {
                            BidHistory bid = bidHistoryRepository.findById(result.getBidNo()).orElse(null);
                            if (bid != null) {
                                Product product = productRepository.findById(bid.getProductNo()).orElse(null);
                                if (product != null) {
                                    productNo = product.getProductNo();
                                    productTitle = product.getTitle();
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    return ReviewResponseDto.from(review, writer.getNickname(), productNo, productTitle);
                }).toList();
    }

    /**
     * 리뷰 숨김 처리
     * 수신자(판매자) 본인만 가능
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
