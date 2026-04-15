package com.javajava.project.domain.auction.service;

import com.javajava.project.domain.auction.dto.AuctionResultResponseDto;
import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.entity.ProductImage;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.global.sse.SseService;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.product.repository.ProductImageRepository;
import com.javajava.project.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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
    private final NotificationService notificationService;
    private final SseService sseService;

    @Override
    public AuctionResultResponseDto getAuctionResultByProductNo(Long productNo, Long memberNo) {
        // 1. 해당 상품의 낙찰 입찰 기록 조회
        BidHistory winnerBid = bidHistoryRepository
                .findFirstByProductNoAndIsWinnerOrderByBidPriceDesc(productNo, 1)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품의 낙찰 기록이 없습니다."));

        // 2. 낙찰자 본인 확인
        if (!winnerBid.getMemberNo().equals(memberNo)) {
            throw new IllegalStateException("낙찰자 본인만 조회할 수 있습니다.");
        }

        // 3. AuctionResult 조회
        AuctionResult result = auctionResultRepository.findFirstByBidNo(winnerBid.getBidNo())
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
                .toList();

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
                .deliveryAddrRoad(result.getDeliveryAddrRoad())
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

        // ─────────────────────────────────────────────────────────────────
        // [수정] 구매자 포인트를 여기서 차감하지 않음.
        //
        // 입찰 시점에 이미 낙찰가만큼 차감(에스크로)되어 있음.
        // processPayment는 그 에스크로를 판매자에게 이전하는 역할만 함.
        // 구매자를 다시 차감하면 낙찰가를 두 번 내는 이중 청구 버그 발생.
        // ─────────────────────────────────────────────────────────────────

        // 에스크로(입찰 시 차감된 금액)를 판매자에게 지급
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
                ? (addressDetail != null && !addressDetail.isBlank() ? address + " " + addressDetail
                        : address)
                : addressDetail;
        result.setDeliveryAddrDetail(fullAddr);

        // 판매자 포인트 실시간 반영 (SSE)
        sseService.sendPointUpdate(seller.getMemberNo(), seller.getPoints());

        // 판매자 알림 — 결제 완료
        try {
            notificationService.sendAndSaveNotification(
                    seller.getMemberNo(), "activity",
                    "구매자가 [" + product.getTitle() + "] 결제를 완료했습니다. 상품 준비를 시작해 주세요.",
                    "/products/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[AuctionResult] 판매자 결제완료 알림 전송 실패: {}", e.getMessage());
        }
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

        // 입찰/상품 정보 조회
        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        // 구매확정 시 양쪽 매너온도 소폭 상승 (+0.2)
        Member buyer = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));
        Member seller = memberRepository.findById(product.getSellerNo())
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
        buyer.setMannerTemp(Math.min(100, buyer.getMannerTemp() + 0.2));
        seller.setMannerTemp(Math.min(100, seller.getMannerTemp() + 0.2));

        // 판매자 알림 — 구매 확정 (포인트 정산 안내)
        try {
            notificationService.sendAndSaveNotification(
                    product.getSellerNo(), "activity",
                    "구매자가 [" + product.getTitle() + "] 구매를 확정하여 "
                            + String.format("%,d", bid.getBidPrice()) + "포인트가 정산되었습니다.",
                    "/products/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[AuctionResult] 판매자 구매확정 알림 전송 실패: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancelTransaction(Long resultNo, Long memberNo) {
        AuctionResult result = getResultAndValidateOwner(resultNo, memberNo);

        if ("구매확정".equals(result.getStatus())) {
            throw new IllegalStateException("이미 구매 확정된 거래는 취소할 수 없습니다.");
        }
        if ("거래취소".equals(result.getStatus())) {
            throw new IllegalStateException("이미 취소된 거래입니다.");
        }

        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        // memberNo 오름차순으로 락 획득 (데드락 방지)
        Member buyer;
        Member sellerForRefund = null;

        if ("결제완료".equals(result.getStatus())) {
            // 결제완료 상태: 판매자에게서 회수 + 구매자에게 환불
            Long sellerNo = product.getSellerNo();
            if (sellerNo < memberNo) {
                sellerForRefund = memberRepository.findByIdWithLock(sellerNo)
                        .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
                buyer = memberRepository.findByIdWithLock(memberNo)
                        .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));
            } else {
                buyer = memberRepository.findByIdWithLock(memberNo)
                        .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));
                sellerForRefund = memberRepository.findByIdWithLock(sellerNo)
                        .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
            }

            // 판매자 포인트 회수
            sellerForRefund.setPoints(sellerForRefund.getPoints() - bid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(sellerForRefund.getMemberNo())
                    .type("거래취소회수")
                    .amount(-bid.getBidPrice())
                    .balance(sellerForRefund.getPoints())
                    .reason("[" + product.getTitle() + "] 거래 취소로 인한 낙찰 대금 회수")
                    .build());
            sseService.sendPointUpdate(sellerForRefund.getMemberNo(), sellerForRefund.getPoints());

            try {
                notificationService.sendAndSaveNotification(
                        sellerForRefund.getMemberNo(), "activity",
                        "구매자가 [" + product.getTitle() + "] 거래를 취소하여 낙찰 대금이 회수되었습니다.",
                        "/products/" + product.getProductNo());
            } catch (Exception e) {
                log.warn("[AuctionResult] 판매자 거래취소 알림 전송 실패: {}", e.getMessage());
            }
        } else {
            // 배송대기 상태: 에스크로(구매자 입찰 차감금액) 환불만
            buyer = memberRepository.findByIdWithLock(memberNo)
                    .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));
        }

        // 구매자 포인트 환불 (에스크로 반환)
        buyer.setPoints(buyer.getPoints() + bid.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(buyer.getMemberNo())
                .type("거래취소환불")
                .amount(bid.getBidPrice())
                .balance(buyer.getPoints())
                .reason("[" + product.getTitle() + "] 거래 취소로 인한 환불")
                .build());
        sseService.sendPointUpdate(buyer.getMemberNo(), buyer.getPoints());

        // 거래 취소 시 구매자 매너온도 하락 (-0.5, 최저 0도)
        buyer.setMannerTemp(Math.max(0, buyer.getMannerTemp() - 0.5));

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
