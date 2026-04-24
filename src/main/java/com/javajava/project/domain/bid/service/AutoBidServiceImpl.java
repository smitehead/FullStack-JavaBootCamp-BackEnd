package com.javajava.project.domain.bid.service;

import com.javajava.project.domain.bid.dto.AutoBidRequestDto;
import com.javajava.project.domain.bid.dto.AutoBidResponseDto;
import com.javajava.project.domain.bid.entity.AutoBid;
import com.javajava.project.domain.bid.entity.BidHistory;
import com.javajava.project.domain.bid.event.AutoBidTriggerEvent;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.point.entity.PointHistory;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.auction.scheduler.AuctionClosingService;
import com.javajava.project.domain.auction.scheduler.AuctionExpiryWatchdog;
import com.javajava.project.domain.bid.repository.AutoBidRepository;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.point.repository.PointHistoryRepository;
import com.javajava.project.domain.product.repository.ProductRepository;
import com.javajava.project.global.sse.SseService;
import com.javajava.project.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoBidServiceImpl implements AutoBidService {

    private final AutoBidRepository autoBidRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SseService sseService;
    private final NotificationService notificationService;
    private final AuctionClosingService auctionClosingService;
    private final AuctionExpiryWatchdog auctionExpiryWatchdog;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AutoBidResponseDto registerAutoBid(Long memberNo, AutoBidRequestDto dto) {
        Product product = productRepository.findById(dto.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        if (product.getStatus() != 0 || product.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("이미 종료된 경매입니다.");
        }
        if (product.getSellerNo().equals(memberNo)) {
            throw new IllegalStateException("본인이 등록한 상품에는 자동입찰을 설정할 수 없습니다.");
        }
        // 재입찰 차단: 이 상품에 취소 이력이 있는 회원은 영구 차단
        if (bidHistoryRepository.existsByProductNoAndMemberNoAndIsCancelled(
                dto.getProductNo(), memberNo, 1)) {
            throw new IllegalStateException("입찰을 취소한 상품에는 다시 입찰할 수 없습니다.");
        }
        long minRequired = product.getCurrentPrice() + product.getMinBidUnit();
        if (dto.getMaxPrice() < minRequired) {
            throw new IllegalArgumentException("자동입찰 한도는 최소 " + minRequired + "원 이상이어야 합니다.");
        }

        // 이미 더 높은 상한가의 자동입찰이 등록된 경우 차단
        autoBidRepository.findActiveByProductNo(dto.getProductNo()).stream()
                .filter(a -> !a.getMemberNo().equals(memberNo))
                .findFirst()
                .ifPresent(existing -> {
                    if (existing.getMaxPrice() == dto.getMaxPrice()) {
                        throw new IllegalStateException(
                                "동일한 금액(" + existing.getMaxPrice() + "원)의 자동입찰이 선착순으로 이미 설정되어 있습니다. " +
                                "자동입찰 한도를 " + (existing.getMaxPrice() + product.getMinBidUnit()) + "원 이상으로 설정해주세요.");
                    }
                    if (existing.getMaxPrice() > dto.getMaxPrice()) {
                        throw new IllegalStateException(
                                "이미 더 높은 자동입찰(" + existing.getMaxPrice() + "원)이 설정되어 있습니다. " +
                                "자동입찰 한도를 " + (existing.getMaxPrice() + product.getMinBidUnit()) + "원 이상으로 설정해주세요.");
                    }
                });

        AutoBid autoBid = autoBidRepository
                .findByMemberNoAndProductNoAndIsActive(memberNo, dto.getProductNo(), 1)
                .orElse(null);

        if (autoBid != null) {
            autoBid.setMaxPrice(dto.getMaxPrice());
            autoBid.setUpdatedAt(LocalDateTime.now());
        } else {
            autoBid = AutoBid.builder()
                    .memberNo(memberNo)
                    .productNo(dto.getProductNo())
                    .maxPrice(dto.getMaxPrice())
                    .build();
        }
        autoBidRepository.save(autoBid);

        // 등록 즉시 경쟁 해소 — 커밋 후 AFTER_COMMIT 리스너가 실행.
        // 동일 트랜잭션에서 triggerAutoBids 를 직접 호출하면:
        // ① 내부 예외 시 registerAutoBid 트랜잭션 전체 롤백(rollback-only 오염)
        // ② Product 재락 시 1차 캐시 오염 위험
        eventPublisher.publishEvent(new AutoBidTriggerEvent(dto.getProductNo(), product.getCurrentPrice(), null));

        return toDto(autoBid);
    }

    @Override
    @Transactional
    public void cancelAutoBid(Long memberNo, Long productNo) {
        autoBidRepository
                .findByMemberNoAndProductNoAndIsActive(memberNo, productNo, 1)
                .ifPresent(autoBid -> {
                    autoBid.setIsActive(0);
                    autoBid.setUpdatedAt(LocalDateTime.now());
                    autoBidRepository.save(autoBid);
                });
    }

    /**
     * 자동입찰 경쟁 해소 (한 트랜잭션 내에서 승자를 결정하고 단 1회 입찰 처리)
     *
     * <p>알고리즘:
     * <ol>
     *   <li>활성 자동입찰을 maxPrice 내림차순으로 조회 (triggerMemberNo 제외)</li>
     *   <li>승자 = 가장 높은 maxPrice 보유자</li>
     *   <li>최종 낙찰가 = min(2위.maxPrice + minUnit, 1위.maxPrice), 단 minNextBid 이상</li>
     *   <li>2위 이하 전원 자동입찰 비활성화 + 실패 알림</li>
     *   <li>승자 1회 입찰 처리 (포인트 차감 + 이전 입찰자 환불)</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional
    public boolean triggerAutoBids(Long productNo, Long currentPrice, Long triggerMemberNo) {
        Product product = productRepository.findByIdWithLock(productNo).orElse(null);
        if (product == null) return false;
        if (product.getStatus() != 0 || product.getEndTime().isBefore(LocalDateTime.now())) return false;

        // 활성 자동입찰 목록 (maxPrice 내림차순), 수동 입찰자 제외
        List<AutoBid> autoBids = autoBidRepository.findActiveByProductNo(productNo);
        if (triggerMemberNo != null) {
            autoBids = autoBids.stream()
                    .filter(a -> !a.getMemberNo().equals(triggerMemberNo))
                    .toList();
        }
        if (autoBids.isEmpty()) return false;

        // 현재 최고 입찰 정보
        Optional<BidHistory> lastBidOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(productNo, 0);
        Long currentTopBidderNo = lastBidOpt.map(BidHistory::getMemberNo).orElse(null);
        long minNextBid = product.getCurrentPrice() + product.getMinBidUnit();

        // ── 1. 승자 후보 결정 ─────────────────────────────────────
        // maxPrice 내림차순 정렬이므로 첫 번째 = 최고 한도 보유자
        AutoBid winner = autoBids.get(0);

        // 이미 최고 입찰자 + 최고 한도 → 다른 자동입찰자들은 절대 이길 수 없으므로 전원 비활성화
        if (winner.getMemberNo().equals(currentTopBidderNo)) {
            for (int i = 1; i < autoBids.size(); i++) {
                deactivateWithNotification(autoBids.get(i), product,
                        "더 높은 자동입찰에 의해 자동입찰이 취소되었습니다.");
            }
            return false;
        }

        // 승자 한도가 최소 입찰가에 미달 → 승자 포함 전원 실패
        // triggerMemberNo != null 이면 수동 입찰이 상한가를 초과한 경우
        if (winner.getMaxPrice() < minNextBid) {
            String reason = triggerMemberNo != null
                    ? "자동입찰이 상위입찰에 의해 취소되었습니다."
                    : "더 높은 자동입찰에 의해 자동입찰이 취소되었습니다.";
            for (AutoBid ab : autoBids) {
                deactivateWithNotification(ab, product, reason);
            }
            return false;
        }

        // ── 2. 최종 낙찰가 계산 ──────────────────────────────────
        // 2위 maxPrice 기준으로 승자가 최소한으로 높게 입찰
        long finalPrice;
        if (autoBids.size() >= 2) {
            long runnerUpMax = autoBids.get(1).getMaxPrice();
            // 낙찰가 = min(2위.max + 단위, 승자.max), 단 minNextBid 이상 보장
            finalPrice = Math.max(runnerUpMax + product.getMinBidUnit(), minNextBid);
            finalPrice = Math.min(finalPrice, winner.getMaxPrice());
        } else {
            finalPrice = minNextBid;
        }

        // ── 3. 2위 이하 전원 실패 처리 ───────────────────────────
        for (int i = 1; i < autoBids.size(); i++) {
            deactivateWithNotification(autoBids.get(i), product,
                    "더 높은 자동입찰에 의해 자동입찰이 취소되었습니다.");
        }

        // ── 4. 승자 입찰 실행 ─────────────────────────────────────
        Long winnerNo = winner.getMemberNo();

        // 멤버 비관적 락 (데드락 방지: memberNo 오름차순)
        Member winnerMember;
        Member previousBidder = null;

        if (currentTopBidderNo != null && currentTopBidderNo < winnerNo) {
            previousBidder = memberRepository.findByIdWithLock(currentTopBidderNo).orElse(null);
            winnerMember = memberRepository.findByIdWithLock(winnerNo)
                    .orElseThrow(() -> new IllegalStateException("자동입찰자를 찾을 수 없습니다."));
        } else if (currentTopBidderNo != null) {
            winnerMember = memberRepository.findByIdWithLock(winnerNo)
                    .orElseThrow(() -> new IllegalStateException("자동입찰자를 찾을 수 없습니다."));
            previousBidder = memberRepository.findByIdWithLock(currentTopBidderNo).orElse(null);
        } else {
            winnerMember = memberRepository.findByIdWithLock(winnerNo)
                    .orElseThrow(() -> new IllegalStateException("자동입찰자를 찾을 수 없습니다."));
        }

        // 포인트 부족 시 승자도 실패
        if (winnerMember.getPoints() < finalPrice) {
            deactivateWithNotification(winner, product, "포인트 부족으로 자동입찰이 취소되었습니다.");
            return false;
        }

        // 이전 최고 입찰자 환불
        if (previousBidder != null && lastBidOpt.isPresent()) {
            BidHistory lastBid = lastBidOpt.get();
            previousBidder.setPoints(previousBidder.getPoints() + lastBid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(previousBidder.getMemberNo())
                    .type("입찰환불")
                    .amount(lastBid.getBidPrice())
                    .balance(previousBidder.getPoints())
                    .reason("[" + product.getTitle() + "] 자동입찰 발생으로 인한 환불")
                    .build());
            try { sseService.sendPointUpdate(previousBidder.getMemberNo(), previousBidder.getPoints()); }
            catch (Exception e) { log.warn("[AutoBid] SSE 실패: {}", e.getMessage()); }
        }

        // 승자 포인트 차감
        winnerMember.setPoints(winnerMember.getPoints() - finalPrice);
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(winnerNo)
                .type("입찰차감")
                .amount(-finalPrice)
                .balance(winnerMember.getPoints())
                .reason("[" + product.getTitle() + "] 자동입찰 참여")
                .build());
        try { sseService.sendPointUpdate(winnerNo, winnerMember.getPoints()); }
        catch (Exception e) { log.warn("[AutoBid] SSE 실패: {}", e.getMessage()); }

        // 즉시구매가 도달 여부 확인 — 도달 시 buyoutPrice에 맞춰 cap
        boolean buyoutTriggered = product.getBuyoutPrice() != null && finalPrice >= product.getBuyoutPrice();
        if (buyoutTriggered) {
            finalPrice = product.getBuyoutPrice();
        }

        // 상품 갱신
        product.setCurrentPrice(finalPrice);
        product.setBidCount(product.getBidCount() + 1);

        BidHistory savedBid = bidHistoryRepository.save(BidHistory.builder()
                .productNo(productNo)
                .memberNo(winnerNo)
                .bidPrice(finalPrice)
                .bidTime(LocalDateTime.now())
                .isAuto(1)
                .isCancelled(0)
                .isWinner(0)
                .build());

        if (buyoutTriggered) {
            // 경매 즉시 종료 (동일 트랜잭션 합류)
            auctionClosingService.closeDueToBuyout(product, savedBid);
            auctionExpiryWatchdog.cancel(productNo);
            try { sseService.broadcastBuyoutEnded(productNo, finalPrice, winnerNo); }
            catch (Exception e) { log.warn("[AutoBid] buyout SSE 실패: {}", e.getMessage()); }
            log.info("[AutoBid] 즉시구매가 도달 종료: productNo={}, winner={}, price={}", productNo, winnerNo, finalPrice);
            return true;
        }

        try { sseService.broadcastPriceUpdate(productNo, finalPrice, winnerNo); }
        catch (Exception e) { log.warn("[AutoBid] 브로드캐스트 실패: {}", e.getMessage()); }

        log.info("[AutoBid] 완료: productNo={}, winner={}, finalPrice={}", productNo, winnerNo, finalPrice);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AutoBidResponseDto> getActiveAutoBid(Long memberNo, Long productNo) {
        return autoBidRepository
                .findByMemberNoAndProductNoAndIsActive(memberNo, productNo, 1)
                .map(this::toDto);
    }

    /**
     * 자동입찰 비활성화 + 알림 전송
     */
    private void deactivateWithNotification(AutoBid autoBid, Product product, String message) {
        autoBid.setIsActive(0);
        autoBid.setUpdatedAt(LocalDateTime.now());
        autoBidRepository.save(autoBid);
        try {
            notificationService.sendAndSaveNotification(
                    autoBid.getMemberNo(), "bid",
                    "[" + product.getTitle() + "] " + message,
                    "/products/" + product.getProductNo());
        } catch (Exception e) {
            log.warn("[AutoBid] 알림 전송 오류: {}", e.getMessage());
        }
    }

    private AutoBidResponseDto toDto(AutoBid a) {
        return AutoBidResponseDto.builder()
                .autoBidNo(a.getAutoBidNo())
                .memberNo(a.getMemberNo())
                .productNo(a.getProductNo())
                .maxPrice(a.getMaxPrice())
                .isActive(a.getIsActive())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
