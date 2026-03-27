package com.javajava.project.service;

import com.javajava.project.dto.AuctionResultResponseDto;
import com.javajava.project.entity.AuctionResult;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Member;
import com.javajava.project.entity.PointHistory;
import com.javajava.project.entity.Product;
import com.javajava.project.entity.ProductImage;
import com.javajava.project.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionResultServiceImpl implements AuctionResultService {

    private final AuctionResultRepository auctionResultRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ProductImageRepository productImageRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Override
    public AuctionResultResponseDto getAuctionResultByProductNo(Long productNo, Long memberNo) {
        // 1. 해당 상품의 낙찰 입찰 기록 조회
        BidHistory winnerBid = bidHistoryRepository.findWinnerByProductNo(productNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품의 낙찰 기록이 없습니다."));

        // 2. 낙찰자 본인 확인
        if (!winnerBid.getMemberNo().equals(memberNo)) {
            throw new IllegalStateException("낙찰자 본인만 조회할 수 있습니다.");
        }

        // 3. AuctionResult 조회
        AuctionResult result = auctionResultRepository.findByBidNo(winnerBid.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("낙찰 결과를 찾을 수 없습니다."));

        // 4. 상품 정보 조회
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 5. 판매자 정보 조회
        Member seller = memberRepository.findById(product.getSellerNo())
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));

        // 6. 이미지 조회
        List<ProductImage> images = productImageRepository.findByProductNoOrderByIsMainDesc(productNo);
        List<String> imageUrls = images.stream()
                .map(img -> "/api/images/" + img.getUuidName())
                .collect(Collectors.toList());

        return AuctionResultResponseDto.builder()
                .resultNo(result.getResultNo())
                .status(result.getStatus())
                .confirmedAt(result.getConfirmedAt())
                .productNo(product.getProductNo())
                .title(product.getTitle())
                .description(product.getDescription())
                .finalPrice(winnerBid.getBidPrice())
                .tradeType(product.getTradeType())
                .location(product.getTradeAddrDetail())
                .images(imageUrls)
                .seller(AuctionResultResponseDto.SellerInfo.builder()
                        .sellerNo(seller.getMemberNo())
                        .nickname(seller.getNickname())
                        .mannerTemp(seller.getMannerTemp())
                        .build())
                .deliveryEmdNo(result.getDeliveryEmdNo())
                .deliveryAddrDetail(result.getDeliveryAddrDetail())
                .build();
    }

    @Override
    @Transactional
    public void processPayment(Long resultNo, Long memberNo, String address, String addressDetail) {
        AuctionResult result = getResultAndValidateOwner(resultNo, memberNo);

        if (!"배송대기".equals(result.getStatus())) {
            throw new IllegalStateException("현재 상태에서는 결제할 수 없습니다. 현재 상태: " + result.getStatus());
        }

        // 입찰 기록 → 상품 → 판매자 조회
        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));
        Member seller = memberRepository.findById(product.getSellerNo())
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));

        // 낙찰 금액을 판매자 포인트에 지급
        seller.setPoints(seller.getPoints() + bid.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(seller.getMemberNo())
                .type("낙찰대금수령")
                .amount(bid.getBidPrice())
                .balance(seller.getPoints())
                .reason("[" + product.getTitle() + "] 낙찰 대금 수령")
                .build());

        result.setStatus("결제완료");
        String fullAddr = (address != null && !address.isBlank())
                ? (addressDetail != null && !addressDetail.isBlank() ? address + " " + addressDetail : address)
                : addressDetail;
        result.setDeliveryAddrDetail(fullAddr);
    }

    @Override
    @Transactional
    public void confirmPurchase(Long resultNo, Long memberNo) {
        AuctionResult result = getResultAndValidateOwner(resultNo, memberNo);

        if (!"결제완료".equals(result.getStatus())) {
            throw new IllegalStateException("결제 완료 상태에서만 구매 확정이 가능합니다.");
        }

        result.setStatus("구매확정");
        result.setConfirmedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void cancelTransaction(Long resultNo, Long memberNo) {
        AuctionResult result = getResultAndValidateOwner(resultNo, memberNo);

        if ("구매확정".equals(result.getStatus())) {
            throw new IllegalStateException("이미 구매 확정된 거래는 취소할 수 없습니다.");
        }

        result.setStatus("거래취소");
    }

    /**
     * AuctionResult를 조회하고 낙찰자 본인인지 검증
     */
    private AuctionResult getResultAndValidateOwner(Long resultNo, Long memberNo) {
        AuctionResult result = auctionResultRepository.findById(resultNo)
                .orElseThrow(() -> new IllegalArgumentException("낙찰 결과를 찾을 수 없습니다."));

        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));

        if (!bid.getMemberNo().equals(memberNo)) {
            throw new IllegalStateException("본인의 낙찰 건만 처리할 수 있습니다.");
        }

        return result;
    }
}
