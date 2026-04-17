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

        long pool = product.getPenaltyPool() != null ? product.getPenaltyPool() : 0L;
        long buyerCashback = pool / 2;

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
                .buyerCashback(buyerCashback)
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

            // penaltyPool 분배: 구매자 50% 캐시백 + 판매자 50% 보상
            long pool = product.getPenaltyPool() != null ? product.getPenaltyPool() : 0L;
            if (pool > 0) {
                long buyerShare  = pool / 2;
                long sellerShare = pool - buyerShare;

                buyer.setPoints(buyer.getPoints() + buyerShare);
                pointHistoryRepository.save(PointHistory.builder()
                        .memberNo(buyer.getMemberNo())
                        .type("위약금보상")
                        .amount(buyerShare)
                        .balance(buyer.getPoints())
                        .reason("[" + product.getTitle() + "] 입찰 취소 위약금 풀 보상 (구매자 50%)")
                        .build());

                seller.setPoints(seller.getPoints() + sellerShare);
                pointHistoryRepository.save(PointHistory.builder()
                        .memberNo(seller.getMemberNo())
                        .type("위약금보상")
                        .amount(sellerShare)
                        .balance(seller.getPoints())
                        .reason("[" + product.getTitle() + "] 입찰 취소 위약금 풀 보상 (판매자 50%)")
                        .build());

                product.setPenaltyPool(0L);
                sseService.sendPointUpdate(buyer.getMemberNo(), buyer.getPoints());
                log.info("[AuctionResult] penaltyPool 분배: productNo={}, 구매자={}P, 판매자={}P",
                        product.getProductNo(), buyerShare, sellerShare);
            }

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

        // ── 강제 승계 취소: 매너온도 패널티 면제 + penaltyPool 전액 판매자 지급 ──
        // 본인 의사 없이 자동 승계된 낙찰자이므로 매너 패널티를 부과하지 않는다.
        // 대신 원래 입찰 취소자의 위약금(penaltyPool)을 판매자에게 전액 보상한다.
        long pool = product.getPenaltyPool() != null ? product.getPenaltyPool() : 0L;
        if (pool > 0) {
            seller.setPoints(seller.getPoints() + pool);
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(seller.getMemberNo())
                    .type("위약금보상")
                    .amount(pool)
                    .balance(seller.getPoints())
                    .reason("[" + product.getTitle() + "] 강제 승계 낙찰 취소 — 입찰 취소자 위약금 풀 전액 보상")
                    .build());
            product.setPenaltyPool(0L);
            sseService.sendPointUpdate(seller.getMemberNo(), seller.getPoints());
            log.info("[AuctionResult] 강제승계 취소 — penaltyPool {}P 판매자 전액 지급: productNo={}",
                    pool, product.getProductNo());
        }

        result.setStatus("거래취소");
        log.info("[AuctionResult] 강제승계 낙찰 취소 완료 (패널티 없음): resultNo={}, memberNo={}",
                resultNo, memberNo);
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
