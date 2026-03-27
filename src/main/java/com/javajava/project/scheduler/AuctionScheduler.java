package com.javajava.project.scheduler;

import com.javajava.project.entity.AuctionResult;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.AuctionResultRepository;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.ProductRepository;
import com.javajava.project.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final NotificationService notificationService;

    /**
     * 매 1분마다 종료된 경매를 체크하여 낙찰 처리
     * - status=0(진행중) 이면서 endTime이 지난 상품을 대상으로 함
     */
    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 종료 시간이 지났지만 아직 진행 중(status=0)인 상품 조회
        List<Product> expiredProducts = productRepository.findByEndTimeBeforeAndStatus(now, 0);

        if (expiredProducts.isEmpty()) {
            return;
        }

        log.info("[Scheduler] {}개의 종료 대상 경매를 발견했습니다.", expiredProducts.size());

        for (Product product : expiredProducts) {

            // 2. 해당 상품의 유효한 최고가 입찰 기록 조회
            Optional<BidHistory> winningBidOpt = bidHistoryRepository
                    .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(product.getProductNo(), 0);

            if (winningBidOpt.isPresent()) {
                BidHistory winningBid = winningBidOpt.get();

                // 3. 입찰 기록에 낙찰 확정 표시
                winningBid.setIsWinner(1);

                // 4. 상품 상태를 completed(1)로 변경 + 낙찰자 기록
                product.setStatus(1);
                product.setWinnerNo(winningBid.getMemberNo());

                // 5. 낙찰 결과 저장
                auctionResultRepository.save(AuctionResult.builder()
                        .bidNo(winningBid.getBidNo())
                        .status("배송대기")
                        .build());

                log.info("[Scheduler] 상품 번호 {} 낙찰 완료 (입찰번호: {}, 낙찰자: {})",
                        product.getProductNo(), winningBid.getBidNo(), winningBid.getMemberNo());

                String productLink = "/product/" + product.getProductNo();
                String wonLink = "/won/" + product.getProductNo();

                // 6. 낙찰자 알림 — 낙찰 성공 + 결제 요청
                try {
                    notificationService.sendAndSaveNotification(
                            winningBid.getMemberNo(), "bid",
                            "축하합니다! [" + product.getTitle() + "] 경매에 최종 낙찰되었습니다.", wonLink);
                    notificationService.sendAndSaveNotification(
                            winningBid.getMemberNo(), "bid",
                            "낙찰받으신 [" + product.getTitle() + "]의 결제를 진행해 주세요. (24시간 내 미결제 시 취소 가능)", wonLink);
                } catch (Exception e) {
                    log.warn("[Scheduler] 낙찰자 알림 전송 실패: {}", e.getMessage());
                }

                // 7. 판매자 알림 — 낙찰 완료
                try {
                    notificationService.sendAndSaveNotification(
                            product.getSellerNo(), "bid",
                            "판매 중인 [" + product.getTitle() + "]이 최종 낙찰되었습니다.", productLink);
                } catch (Exception e) {
                    log.warn("[Scheduler] 판매자 알림 전송 실패: {}", e.getMessage());
                }

                // 8. 낙찰 실패자 알림
                try {
                    List<Long> loserMemberNos = bidHistoryRepository.findDistinctBiddersExcluding(
                            product.getProductNo(), winningBid.getMemberNo());
                    for (Long loserNo : loserMemberNos) {
                        notificationService.sendAndSaveNotification(
                                loserNo, "bid",
                                "아쉽게도 [" + product.getTitle() + "] 경매가 종료되었습니다. 다음 기회를 노려보세요!", productLink);
                    }
                } catch (Exception e) {
                    log.warn("[Scheduler] 낙찰 실패자 알림 전송 실패: {}", e.getMessage());
                }

            } else {
                // 입찰자 없음 → 유찰(canceled=2) 처리
                product.setStatus(2);
                log.info("[Scheduler] 상품 번호 {} 유찰 처리 (입찰자 없음)", product.getProductNo());
            }
        }
    }
}