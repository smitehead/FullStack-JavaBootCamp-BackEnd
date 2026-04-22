package com.javajava.project.domain.bid.event;

import com.javajava.project.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 최고 입찰자 취소 완료 이벤트 리스너.
 *
 * @TransactionalEventListener(AFTER_COMMIT):
 * - BidCancelService 트랜잭션이 완전히 커밋된 후에만 실행됨
 * - DB에 취소 데이터가 확정된 상태에서 알림 발송 → 알림 오발송 없음
 * - 커밋 실패 시 이 리스너 자체가 실행되지 않음
 *
 * @Transactional(REQUIRES_NEW):
 * - AFTER_COMMIT 이후엔 원래 트랜잭션이 종료된 상태
 * - 알림 저장(DB 접근)을 위해 새 트랜잭션 시작
 * - 알림 발송 실패가 비즈니스 롤백을 유발하지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidCancelNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBidCancelled(BidCancelledEvent event) {
        String productLink = "/products/" + event.productNo();

        // 취소한 입찰자 본인 알림: 위약금 차감 사실 고지
        try {
            notificationService.sendAndSaveNotification(
                    event.cancelledBidderNo(), "bid",
                    "[" + event.productTitle() + "] 입찰 취소가 완료되었으며, 규정에 따라 위약금 5%가 차감되었습니다.",
                    productLink);
        } catch (Exception e) {
            log.warn("[BidCancelNotification] 취소자 알림 실패 (상품 {}): {}", event.productNo(), e.getMessage());
        }

        // 판매자 알림: 입찰 취소 사실 + 차순위 승계 안내 (위약금 언급 제거)
        try {
            notificationService.sendAndSaveNotification(
                    event.sellerNo(), "bid",
                    "최상위 입찰자의 사정으로 [" + event.productTitle() + "] 입찰이 취소되어 차순위 입찰자가 최고 입찰자가 되었습니다.",
                    productLink);
        } catch (Exception e) {
            log.warn("[BidCancelNotification] 판매자 알림 실패 (상품 {}): {}", event.productNo(), e.getMessage());
        }

        // 차순위(2등) 입찰자 알림: 최고 입찰자 지위 승계 안내
        if (event.successorBidderNo() != null) {
            try {
                notificationService.sendAndSaveNotification(
                        event.successorBidderNo(), "bid",
                        "상위 입찰자의 취소로 [" + event.productTitle() + "] 최고 입찰자 지위가 승계되었습니다.",
                        productLink);
            } catch (Exception e) {
                log.warn("[BidCancelNotification] 차순위자 알림 실패 (상품 {}): {}", event.productNo(), e.getMessage());
            }
        }
    }
}
