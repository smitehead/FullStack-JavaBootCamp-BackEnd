package com.javajava.project.service;

import com.javajava.project.dto.BidRequestDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Member;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidServiceImpl implements BidService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final BidHistoryRepository bidHistoryRepository;

    /**
     * 입찰 프로세스 실행
     * - 검증(포인트, 최소입찰가, 본인상품여부 등) + 상품 가격 갱신 + 입찰 기록 저장
     */
    @Override
    @Transactional
    public String processBid(BidRequestDto bidDto) {
        // 1. 상품 정보 조회 (동시성 제어를 위해 비관적 락 적용)
        Product product = productRepository.findByIdWithLock(bidDto.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 2. 입찰자 정보 조회
        Member bidder = memberRepository.findById(bidDto.getMemberNo())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 3. 유효성 검증
        // - 경매 마감 여부 확인
        if (product.getIsActive() == 0 || product.getEndTime().isBefore(LocalDateTime.now())) {
            return "이미 종료된 경매입니다.";
        }

        // - 본인 등록 상품 여부 확인
        if (product.getSellerNo().equals(bidDto.getMemberNo())) {
            return "본인이 등록한 상품에는 입찰할 수 없습니다.";
        }

        // - 최소 입찰 금액 검증 (현재가 + 최소입찰단위)
        long minRequiredBid = product.getCurrentPrice() + product.getMinBidUnit();
        if (bidDto.getBidPrice() < minRequiredBid) {
            return "최소 입찰 가능 금액은 " + minRequiredBid + "원입니다.";
        }

        // - 보유 포인트 확인
        if (bidder.getPoints() < bidDto.getBidPrice()) {
            return "보유 포인트가 부족합니다.";
        }

        // 4. 상품 현재가 및 입찰 수 업데이트
        product.setCurrentPrice(bidDto.getBidPrice());
        product.setBidCount(product.getBidCount() + 1);

        // 5. 입찰 기록 저장 (엔티티에 @Builder.Default 설정 필수)
        BidHistory bidHistory = BidHistory.builder()
                .productNo(product.getProductNo())
                .memberNo(bidder.getMemberNo())
                .bidPrice(bidDto.getBidPrice())
                .bidTime(LocalDateTime.now())
                .isAuto(0)
                .isCancelled(0)
                .isWinner(0)
                .build();

        bidHistoryRepository.save(bidHistory);

        return "SUCCESS";
    }

    /**
     * 입찰 기록 조회 (상세 페이지 탭 전용)
     * - JOIN 쿼리를 활용해 입찰 정보와 닉네임을 한 번에 가져옴 (N+1 문제 해결)
     */
    @Override
    public List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo) {
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

    /**
     * 특정 입찰 건 취소 로직
     */
    @Override
    @Transactional
    public void cancelBid(Long bidNo, String reason) {
        BidHistory bid = bidHistoryRepository.findById(bidNo)
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));

        bid.setIsCancelled(1); // 1: 취소됨
        bid.setCancelReason(reason);
        // 필요 시 상품의 currentPrice를 이전 입찰가로 되돌리는 로직 추가 가능
    }

    /**
     * 단순 입찰 저장 (엔티티 기반)
     */
    @Override
    @Transactional
    public void placeBid(BidHistory bid) {
        bidHistoryRepository.save(bid);
    }

    /**
     * 특정 상품의 모든 입찰 내역 조회 (엔티티 기반)
     */
    @Override
    public List<BidHistory> getHistoryByProduct(Long productNo) {
        return bidHistoryRepository.findByProductNoOrderByBidTimeDesc(productNo);
    }
}