package com.javajava.project.domain.auction.service;

import com.javajava.project.domain.auction.dto.AuctionResultResponseDto;
import com.javajava.project.domain.auction.dto.SellerAuctionResultResponseDto;
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
                .isForcePromoted(result.getIsForcePromoted())
                .build();
    }

    /**
     * 구매 확정 (1-step 통합 플로우).
     *
     * <p>입찰 시점에 이미 낙찰가만큼 포인트가 에스크로 차감되어 있으므로,
     * 별도의 결제 단계 없이 구매자가 수령 확인 시 모든 정산을 한 번에 처리한다.
     *
     * <ul>
     *   <li>배송대기 상태: 에스크로 → 판매자 지급 + penaltyPool 분배 + 주소 저장 + 구매확정</li>
     *   <li>결제완료 상태(하위호환): 이미 판매자 지급 완료 → 구매확정만</li>
     * </ul>
     */
    @Override
    @Transactional
    public void confirmPurchase(Long resultNo, Long memberNo, String address, String addressDetail) {
        AuctionResult result = getResultAndValidateOwner(resultNo, memberNo);

        if ("구매확정".equals(result.getStatus())) {
            throw new IllegalStateException("이미 구매 확정된 거래입니다.");
        }
        if ("거래취소".equals(result.getStatus())) {
            throw new IllegalStateException("취소된 거래는 구매 확정할 수 없습니다.");
        }

        // 입찰/상품 정보 조회
        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        // ── 입찰 취소 이력 방어 검증 ──────────────────────────────────────────
        if (bidHistoryRepository.existsByProductNoAndMemberNoAndIsCancelled(
                product.getProductNo(), memberNo, 1)) {
            throw new IllegalStateException("입찰을 취소한 상품은 구매 확정할 수 없습니다.");
        }

        Member seller = memberRepository.findById(product.getSellerNo())
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
        Member buyer = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));

        // ── 배송대기 상태: 에스크로 정산 (구버전 결제완료 상태는 이미 정산 완료) ──
        if ("배송대기".equals(result.getStatus())) {
            // 에스크로(입찰 시 차감된 금액)를 판매자에게 지급
            // 구매자 포인트는 입찰 시점에 이미 차감됨 — 여기서 재차감 없음
            seller.setPoints(seller.getPoints() + bid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(seller.getMemberNo())
                    .type("낙찰대금수령")
                    .amount(bid.getBidPrice())
                    .balance(seller.getPoints())
                    .reason("[" + product.getTitle() + "] 낙찰 대금 수령")
                    .build());

            // 배송지 저장
            String fullAddr = (address != null && !address.isBlank())
                    ? (addressDetail != null && !addressDetail.isBlank() ? address + " " + addressDetail : address)
                    : (addressDetail != null ? addressDetail : "");
            result.setDeliveryAddrDetail(fullAddr);

            sseService.sendPointUpdate(seller.getMemberNo(), seller.getPoints());
        }

        // ── 구매 확정 처리 ─────────────────────────────────────────────────────
        result.setStatus("구매확정");
        result.setConfirmedAt(LocalDateTime.now());

        buyer.setMannerTemp(Math.min(100, buyer.getMannerTemp() + 0.2));
        seller.setMannerTemp(Math.min(100, seller.getMannerTemp() + 0.2));

        try {
            notificationService.sendAndSaveNotification(
                    seller.getMemberNo(), "activity",
                    "구매자가 [" + product.getTitle() + "] 상품 수령을 확인하여 "
                            + String.format("%,d", bid.getBidPrice()) + "P가 정산되었습니다.",
                    "/products/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[AuctionResult] 판매자 구매확정 알림 전송 실패: {}", e.getMessage());
        }

        log.info("[AuctionResult] 구매 확정 완료: resultNo={}, memberNo={}, price={}P",
                resultNo, memberNo, bid.getBidPrice());
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

        // ── 강제 승계 여부 확인 ─────────────────────────────────────────────────
        // isForcePromoted = 0 (일반 낙찰)은 낙찰 취소 불가.
        // isForcePromoted = 1 (입찰 취소로 자동 승계)만 불이익 없이 취소 허용.
        boolean isForcePromoted = Integer.valueOf(1).equals(result.getIsForcePromoted());
        if (!isForcePromoted) {
            throw new IllegalStateException("정상 낙찰 건은 취소할 수 없습니다. 판매자와 채팅으로 문의해 주세요.");
        }

        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        Long sellerNo = product.getSellerNo();

        // memberNo 오름차순으로 락 획득 (데드락 방지)
        Member buyer;
        Member seller;
        if (sellerNo < memberNo) {
            seller = memberRepository.findByIdWithLock(sellerNo)
                    .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
            buyer = memberRepository.findByIdWithLock(memberNo)
                    .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));
        } else {
            buyer = memberRepository.findByIdWithLock(memberNo)
                    .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));
            seller = memberRepository.findByIdWithLock(sellerNo)
                    .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
        }

        if ("결제완료".equals(result.getStatus())) {
            // 결제완료 → 판매자에게서 낙찰 대금 회수 후 구매자 환불
            seller.setPoints(seller.getPoints() - bid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(seller.getMemberNo())
                    .type("거래취소회수")
                    .amount(-bid.getBidPrice())
                    .balance(seller.getPoints())
                    .reason("[" + product.getTitle() + "] 강제 승계 낙찰 취소 — 낙찰 대금 회수")
                    .build());
            sseService.sendPointUpdate(seller.getMemberNo(), seller.getPoints());

            try {
                notificationService.sendAndSaveNotification(
                        seller.getMemberNo(), "activity",
                        "강제 승계 낙찰자가 [" + product.getTitle() + "] 거래를 취소하여 낙찰 대금이 회수되었습니다.",
                        "/products/" + product.getProductNo());
            } catch (Exception e) {
                log.warn("[AuctionResult] 판매자 거래취소 알림 전송 실패: {}", e.getMessage());
            }
        }

        // 구매자 포인트 전액 환불 (에스크로 반환, 패널티 없음)
        buyer.setPoints(buyer.getPoints() + bid.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(buyer.getMemberNo())
                .type("거래취소환불")
                .amount(bid.getBidPrice())
                .balance(buyer.getPoints())
                .reason("[" + product.getTitle() + "] 강제 승계 낙찰 취소 — 패널티 없는 전액 환불")
                .build());
        sseService.sendPointUpdate(buyer.getMemberNo(), buyer.getPoints());

        result.setStatus("거래취소");
        log.info("[AuctionResult] 강제승계 낙찰 취소 완료 (패널티 없음): resultNo={}, memberNo={}",
                resultNo, memberNo);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 판매자 전용 낙찰 결과 조회
    // ────────────────────────────────────────────────────────────────────────────
    @Override
    public SellerAuctionResultResponseDto getAuctionResultBySellerAndProduct(Long productNo, Long sellerNo) {
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        if (!product.getSellerNo().equals(sellerNo)) {
            throw new IllegalStateException("해당 상품의 판매자만 조회할 수 있습니다.");
        }

        BidHistory winnerBid = bidHistoryRepository
                .findFirstByProductNoAndIsWinnerOrderByBidPriceDesc(productNo, 1)
                .orElseThrow(() -> new IllegalArgumentException("낙찰 기록이 없습니다."));

        AuctionResult result = auctionResultRepository.findFirstByBidNo(winnerBid.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("낙찰 결과를 찾을 수 없습니다."));

        Member buyer = memberRepository.findById(winnerBid.getMemberNo())
                .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));

        List<ProductImage> images = productImageRepository.findByProductNoOrderByIsMainDesc(productNo);
        List<String> imageUrls = images.stream()
                .map(img -> "/api/images/" + img.getUuidName())
                .toList();

        return SellerAuctionResultResponseDto.builder()
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
                .buyer(SellerAuctionResultResponseDto.BuyerInfo.builder()
                        .buyerNo(buyer.getMemberNo())
                        .nickname(buyer.getNickname())
                        .mannerTemp(buyer.getMannerTemp())
                        .build())
                .deliveryAddrRoad(result.getDeliveryAddrRoad())
                .deliveryAddrDetail(result.getDeliveryAddrDetail())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 일반 낙찰자 취소 요청 (isForcePromoted=0 → 상태: 취소요청)
    // ────────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void requestCancel(Long resultNo, Long memberNo) {
        AuctionResult result = getResultAndValidateOwner(resultNo, memberNo);

        if ("구매확정".equals(result.getStatus())) {
            throw new IllegalStateException("이미 구매 확정된 거래는 취소할 수 없습니다.");
        }
        if ("거래취소".equals(result.getStatus())) {
            throw new IllegalStateException("이미 취소된 거래입니다.");
        }
        if ("취소요청".equals(result.getStatus())) {
            throw new IllegalStateException("이미 취소 요청을 보냈습니다. 판매자의 승인을 기다려주세요.");
        }
        // 강제 승계자는 /cancel API로 즉시 취소
        if (Integer.valueOf(1).equals(result.getIsForcePromoted())) {
            throw new IllegalStateException("강제 승계 낙찰자는 취소 API를 사용해주세요.");
        }

        result.setStatus("취소요청");

        // 판매자에게 취소 요청 알림
        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        try {
            notificationService.sendAndSaveNotification(
                    product.getSellerNo(), "activity",
                    "구매자가 [" + product.getTitle() + "] 낙찰 취소를 요청했습니다. 판매자 페이지에서 확인해주세요.",
                    "/seller-result/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[AuctionResult] 판매자 취소요청 알림 전송 실패: {}", e.getMessage());
        }

        log.info("[AuctionResult] 일반 낙찰자 취소 요청: resultNo={}, memberNo={}", resultNo, memberNo);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 판매자 취소 승인 (구매자 요청 승인 or 판매자 직접 취소 개시)
    // ────────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void approveCancel(Long resultNo, Long sellerNo) {
        AuctionResult result = auctionResultRepository.findById(resultNo)
                .orElseThrow(() -> new IllegalArgumentException("낙찰 결과를 찾을 수 없습니다."));

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

        if (!product.getSellerNo().equals(sellerNo)) {
            throw new IllegalStateException("해당 상품의 판매자만 취소를 처리할 수 있습니다.");
        }

        Long buyerMemberNo = bid.getMemberNo();

        // 데드락 방지: memberNo 오름차순으로 락 획득
        Member seller;
        Member buyer;
        if (sellerNo < buyerMemberNo) {
            seller = memberRepository.findByIdWithLock(sellerNo)
                    .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
            buyer = memberRepository.findByIdWithLock(buyerMemberNo)
                    .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));
        } else {
            buyer = memberRepository.findByIdWithLock(buyerMemberNo)
                    .orElseThrow(() -> new IllegalArgumentException("구매자 정보를 찾을 수 없습니다."));
            seller = memberRepository.findByIdWithLock(sellerNo)
                    .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
        }

        // '결제완료' 상태면 이미 판매자에게 정산 완료 → 회수 후 환불
        if ("결제완료".equals(result.getStatus())) {
            seller.setPoints(seller.getPoints() - bid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(seller.getMemberNo())
                    .type("거래취소회수")
                    .amount(-bid.getBidPrice())
                    .balance(seller.getPoints())
                    .reason("[" + product.getTitle() + "] 상호 합의 취소 — 낙찰 대금 회수")
                    .build());
            sseService.sendPointUpdate(seller.getMemberNo(), seller.getPoints());
        }

        // 구매자 전액 환불 (에스크로 반환)
        buyer.setPoints(buyer.getPoints() + bid.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(buyer.getMemberNo())
                .type("거래취소환불")
                .amount(bid.getBidPrice())
                .balance(buyer.getPoints())
                .reason("[" + product.getTitle() + "] 상호 합의 취소 — 전액 환불")
                .build());
        sseService.sendPointUpdate(buyer.getMemberNo(), buyer.getPoints());

        result.setStatus("거래취소");

        // 구매자에게 취소 승인 알림
        try {
            notificationService.sendAndSaveNotification(
                    buyerMemberNo, "activity",
                    "판매자가 [" + product.getTitle() + "] 낙찰 취소 요청을 승인했습니다. 포인트가 환불됩니다.",
                    "/won/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[AuctionResult] 구매자 취소승인 알림 전송 실패: {}", e.getMessage());
        }

        log.info("[AuctionResult] 판매자 취소 승인 완료: resultNo={}, sellerNo={}", resultNo, sellerNo);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 판매자 배송지 업데이트 (채팅에서 받은 ADDRESS 메시지 확인 시)
    // ────────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void updateDeliveryAddress(Long productNo, Long sellerNo, String addrRoad, String addrDetail) {
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        if (!product.getSellerNo().equals(sellerNo)) {
            throw new IllegalStateException("해당 상품의 판매자만 배송지를 수정할 수 있습니다.");
        }
        BidHistory winnerBid = bidHistoryRepository
                .findFirstByProductNoAndIsWinnerOrderByBidPriceDesc(productNo, 1)
                .orElseThrow(() -> new IllegalArgumentException("낙찰 기록이 없습니다."));
        AuctionResult result = auctionResultRepository.findFirstByBidNo(winnerBid.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException("낙찰 결과를 찾을 수 없습니다."));

        result.setDeliveryAddrRoad(addrRoad);
        result.setDeliveryAddrDetail(addrDetail);
        log.info("[AuctionResult] 판매자 배송지 업데이트: productNo={}, sellerNo={}", productNo, sellerNo);
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
