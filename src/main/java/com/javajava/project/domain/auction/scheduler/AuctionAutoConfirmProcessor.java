package com.javajava.project.domain.auction.scheduler;

import com.javajava.project.domain.auction.entity.AuctionResult;
import com.javajava.project.domain.auction.repository.AuctionResultRepository;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.domain.platform.entity.PlatformRevenue;
import com.javajava.project.domain.platform.repository.PlatformRevenueRepository;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.repository.ProductRepository;
import com.javajava.project.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 7일 자동 구매 확정 단건 처리 서비스.
 *
 * <p>Self-Invocation 방지를 위해 {@link AuctionAutoConfirmScheduler}에서
 * 분리된 독립 빈. {@code REQUIRES_NEW}로 스케줄러 호출마다 새 트랜잭션을 보장하여
 * 단건 실패가 다른 건에 영향을 주지 않도록 격리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionAutoConfirmProcessor {

    private final AuctionResultRepository auctionResultRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PlatformRevenueRepository platformRevenueRepository;
    private final NotificationService notificationService;
    private final SseService sseService;

    /**
     * 낙찰 후 7일 경과 시 자동 구매 확정 처리 (단건, 독립 트랜잭션).
     *
     * <p>처리 순서:
     * <ol>
     *   <li>멱등성 체크 — 이미 처리된 건 건너뜀</li>
     *   <li>에스크로 정산 — 입찰 시 차감된 낙찰가를 판매자에게 지급</li>
     *   <li>상태 변경 — 배송대기 → 구매확정, confirmedAt 기록</li>
     *   <li>매너온도 +0.2 (구매자·판매자 각각)</li>
     *   <li>SSE 포인트 알림 + DB 알림 발송</li>
     * </ol>
     *
     * @param resultNo 처리할 AuctionResult PK
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoConfirm(Long resultNo) {

        // ── 1. 멱등성 체크 ──────────────────────────────────────────────────────
        AuctionResult result = auctionResultRepository.findById(resultNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AuctionResult를 찾을 수 없습니다: " + resultNo));

        if (!"배송대기".equals(result.getStatus())) {
            log.info("[AutoConfirm] resultNo={} 이미 처리됨 (status={}), 건너뜁니다.",
                    resultNo, result.getStatus());
            return;
        }

        // ── 2. 관련 엔티티 조회 ───────────────────────────────────────────────
        BidHistory bid = bidHistoryRepository.findById(result.getBidNo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "입찰 기록을 찾을 수 없습니다. bidNo=" + result.getBidNo()));
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "상품을 찾을 수 없습니다. productNo=" + bid.getProductNo()));
        Member seller = memberRepository.findById(product.getSellerNo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "판매자를 찾을 수 없습니다. sellerNo=" + product.getSellerNo()));
        Member buyer = memberRepository.findById(bid.getMemberNo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "구매자를 찾을 수 없습니다. memberNo=" + bid.getMemberNo()));

        // ── 3. 에스크로 정산: 수수료 차감 후 판매자에게 정산금 지급 ──────────
        double feeRate = "직거래".equals(product.getTradeType()) ? 0.01 : 0.02;
        String feeLabel = "직거래".equals(product.getTradeType()) ? "1%" : "2%";
        long fee = Math.round(bid.getBidPrice() * feeRate);
        long settlementAmount = bid.getBidPrice() - fee;

        seller.setPoints(seller.getPoints() + settlementAmount);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(seller.getMemberNo())
                .type("낙찰대금수령")
                .amount(settlementAmount)
                .balance(seller.getPoints())
                .reason("[" + product.getTitle() + "] 판매 정산금 (수수료 " + feeLabel + " 제외) — 7일 자동 구매 확정")
                .build());

        platformRevenueRepository.save(PlatformRevenue.builder()
                .amount(fee)
                .reason("낙찰 수수료 (플랫폼 이용료)")
                .sourceMemberNo(seller.getMemberNo())
                .relatedProductNo(product.getProductNo())
                .build());

        // ── 4. 구매 확정 처리 ──────────────────────────────────────────────────
        result.setStatus("구매확정");
        result.setConfirmedAt(LocalDateTime.now());
        product.setStatus(1); // COMPLETED

        buyer.setMannerTemp(Math.min(100.0, buyer.getMannerTemp() + 0.2));
        seller.setMannerTemp(Math.min(100.0, seller.getMannerTemp() + 0.2));

        // ── 5. SSE 포인트 갱신 ────────────────────────────────────────────────
        try {
            sseService.sendPointUpdate(seller.getMemberNo(), seller.getPoints());
        } catch (Exception e) {
            log.warn("[AutoConfirm] 판매자 포인트 SSE 실패 (resultNo={}): {}", resultNo, e.getMessage());
        }

        // ── 6. 알림 발송 (판매자 / 구매자) ────────────────────────────────────
        try {
            notificationService.sendAndSaveNotification(
                    seller.getMemberNo(), "activity",
                    "[" + product.getTitle() + "] 상품이 7일 자동 구매 확정되어 "
                            + String.format("%,d", settlementAmount) + "P가 정산되었습니다.",
                    "/products/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[AutoConfirm] 판매자 알림 실패 (resultNo={}): {}", resultNo, e.getMessage());
        }

        try {
            notificationService.sendAndSaveNotification(
                    buyer.getMemberNo(), "activity",
                    "[" + product.getTitle() + "] 구매가 자동으로 확정되었습니다.",
                    "/won/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[AutoConfirm] 구매자 알림 실패 (resultNo={}): {}", resultNo, e.getMessage());
        }

        log.info("[AutoConfirm] 자동 구매 확정 완료: resultNo={}, buyerNo={}, sellerNo={}, price={}P, settlement={}P",
                resultNo, buyer.getMemberNo(), seller.getMemberNo(), bid.getBidPrice(), settlementAmount);
    }
}
