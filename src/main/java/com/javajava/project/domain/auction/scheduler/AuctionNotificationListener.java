package com.javajava.project.domain.auction.scheduler;

import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 경매 낙찰 완료 이벤트 리스너.
 *
 * @TransactionalEventListener(AFTER_COMMIT):
 * - processOne() 트랜잭션이 완전히 커밋된 후에 실행됨
 * - DB에 낙찰 데이터가 확정된 상태에서 알림 발송
 * - 커밋 실패 시 이 리스너 자체가 실행되지 않으므로 알림 오발송 없음
 *
 * @Transactional(REQUIRES_NEW):
 * - AFTER_COMMIT 이후엔 원래 트랜잭션이 종료된 상태
 * - 낙찰 실패자 조회 (DB 접근)를 위해 새 트랜잭션 시작
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionNotificationListener {

    private final NotificationService notificationService;
    private final BidHistoryRepository bidHistoryRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuctionClosed(AuctionClosedEvent event) {
        String productLink = "/products/" + event.productNo();
        String wonLink = "/won/" + event.productNo();

        // 낙찰자 알림
        try {
            notificationService.sendAndSaveNotification(
                    event.winnerMemberNo(), "bid",
                    "축하합니다! [" + event.productTitle() + "] 경매에 최종 낙찰되었습니다.", wonLink);
            notificationService.sendAndSaveNotification(
                    event.winnerMemberNo(), "bid",
                    "낙찰받으신 [" + event.productTitle() + "]의 결제를 진행해 주세요. (24시간 내 미결제 시 취소 가능)", wonLink);
        } catch (Exception e) {
            log.warn("[Notification] 낙찰자 알림 전송 실패 (상품 {}): {}", event.productNo(), e.getMessage());
        }

        // 판매자 알림
        try {
            notificationService.sendAndSaveNotification(
                    event.sellerNo(), "bid",
                    "판매 중인 [" + event.productTitle() + "]이 최종 낙찰되었습니다.", productLink);
        } catch (Exception e) {
            log.warn("[Notification] 판매자 알림 전송 실패 (상품 {}): {}", event.productNo(), e.getMessage());
        }

        // 낙찰 실패자 알림
        try {
            List<Long> loserMemberNos = bidHistoryRepository.findDistinctBiddersExcluding(
                    event.productNo(), event.winnerMemberNo());
            for (Long loserNo : loserMemberNos) {
                notificationService.sendAndSaveNotification(
                        loserNo, "bid",
                        "아쉽게도 [" + event.productTitle() + "] 경매가 종료되었습니다. 다음 기회를 노려보세요!", productLink);
            }
        } catch (Exception e) {
            log.warn("[Notification] 낙찰 실패자 알림 전송 실패 (상품 {}): {}", event.productNo(), e.getMessage());
        }
    }
}
