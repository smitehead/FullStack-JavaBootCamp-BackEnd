package com.javajava.project.service;

import com.javajava.project.dto.BidRequestDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Member;
import com.javajava.project.entity.PointHistory;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.repository.PointHistoryRepository;
import com.javajava.project.repository.ProductRepository;
import com.javajava.project.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidServiceImpl implements BidService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final PointHistoryRepository pointHistoryRepository; // 포인트 이력 관리를 위해 추가
    private final SseService sseService; // SSE 알림 발송 서비스
    private final NotificationService notificationService; // 알림 저장 및 발송 서비스

    /**
     * 입찰 프로세스 실행 (검증 + 포인트 환불/차감 + 상품 갱신 + 입찰 기록)
     */
    @Override
    @Transactional
    public String processBid(BidRequestDto bidDto) {
        // 1. 상품 정보 조회 (동시성 제어를 위해 비관적 락 적용)
        Product product = productRepository.findByIdWithLock(bidDto.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 2. 현재 입찰자 정보 조회 (포인트 업데이트를 위한 락)
        Member currentBidder = memberRepository.findByIdWithLock(bidDto.getMemberNo())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 3. 유효성 검증
        if (product.getIsActive() == 0 || product.getEndTime().isBefore(LocalDateTime.now())) {
            return "이미 종료된 경매입니다.";
        }
        if (product.getSellerNo().equals(bidDto.getMemberNo())) {
            return "본인이 등록한 상품에는 입찰할 수 없습니다.";
        }
        long minRequiredBid = product.getCurrentPrice() + product.getMinBidUnit();
        if (bidDto.getBidPrice() < minRequiredBid) {
            return "최소 입찰 가능 금액은 " + minRequiredBid + "원입니다.";
        }
        // --- 4. 기존 최고 입찰자 확인 및 환불 예상 금액 계산 ---
        long availablePoints = currentBidder.getPoints();
        Optional<BidHistory> lastBidOpt = bidHistoryRepository
                .findFirstByProductNoAndIsCancelledOrderByBidPriceDesc(product.getProductNo(), 0);

        if (lastBidOpt.isPresent()) {
            BidHistory lastBid = lastBidOpt.get();
            // 현재 최고 입찰자가 본인인 경우, 연속 입찰 방지
            if (lastBid.getMemberNo().equals(currentBidder.getMemberNo())) {
                return "현재 최고 입찰자입니다. 추가 입찰이 불가합니다.";
            }
        }

        if (availablePoints < bidDto.getBidPrice()) {
            return "보유 포인트가 부족합니다.";
        }

        // --- 4-2. 기존 최고 입찰자 실제 환불 로직 ---
        if (lastBidOpt.isPresent()) {
            BidHistory lastBid = lastBidOpt.get();
            
            Member previousBidder = memberRepository.findByIdWithLock(lastBid.getMemberNo())
                    .orElseThrow(() -> new IllegalStateException("이전 입찰자 정보를 찾을 수 없습니다."));

            // 포인트 환불 및 이력 저장
            previousBidder.setPoints(previousBidder.getPoints() + lastBid.getBidPrice());
            pointHistoryRepository.save(PointHistory.builder()
                    .memberNo(previousBidder.getMemberNo())
                    .type("입찰환불")
                    .amount(lastBid.getBidPrice())
                    .balance(previousBidder.getPoints())
                    .reason("[" + product.getTitle() + "] 상위 입찰 발생으로 인한 자동 환불")
                    .build());

            // 이전 입찰자가 현재 입찰자와 다른 경우에만 알림 진행
            if (!lastBid.getMemberNo().equals(currentBidder.getMemberNo())) {
                String messageToPrevBidder = String.format("상위 입찰 발생: %s에 더 높은 입찰가가 등록되었습니다.", product.getTitle());
                notificationService.sendAndSaveNotification(
                        previousBidder.getMemberNo(),
                        "bid",
                        messageToPrevBidder,
                        "/product/" + product.getProductNo()
                );
            }
        }

        // --- 5. 현재 입찰자 포인트 차감 로직 ---
        currentBidder.setPoints(currentBidder.getPoints() - bidDto.getBidPrice());
        pointHistoryRepository.save(PointHistory.builder()
                .memberNo(currentBidder.getMemberNo())
                .type("입찰차감")
                .amount(-bidDto.getBidPrice())
                .balance(currentBidder.getPoints())
                .reason("[" + product.getTitle() + "] 경매 입찰 참여")
                .build());

        // 6. 상품 및 입찰 기록 업데이트
        product.setCurrentPrice(bidDto.getBidPrice());
        product.setBidCount(product.getBidCount() + 1);

        BidHistory newBid = BidHistory.builder()
                .productNo(product.getProductNo())
                .memberNo(currentBidder.getMemberNo())
                .bidPrice(bidDto.getBidPrice())
                .bidTime(LocalDateTime.now())
                .isAuto(0)
                .isCancelled(0)
                .isWinner(0)
                .build();

        bidHistoryRepository.save(newBid);

        // [알림 발송] 판매자에게 새로운 입찰 알림 전송 (명세서 반영)
        String messageToSeller = String.format("새로운 입찰: 등록하신 %s에 새로운 입찰자가 등장했습니다.", product.getTitle());
        notificationService.sendAndSaveNotification(
                product.getSellerNo(),
                "bid",
                messageToSeller,
                "/product/" + product.getProductNo()
        );

        // --- 접속 중인 모든 클라이언트(비로그인 포함)에게 변경된 입찰가 브로드캐스트 ---
        sseService.broadcastPriceUpdate(product.getProductNo(), bidDto.getBidPrice());

        return "SUCCESS";
    }

    /**
     * 입찰 기록 조회 (상세 페이지 탭 전용)
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
     * 입찰 취소 로직
     */
    @Override
    @Transactional
    public void cancelBid(Long bidNo, String reason) {
        BidHistory bid = bidHistoryRepository.findById(bidNo)
                .orElseThrow(() -> new IllegalArgumentException("입찰 기록을 찾을 수 없습니다."));

        bid.setIsCancelled(1);
        bid.setCancelReason(reason);
        
        // 실제 서비스에서는 취소 시 포인트 환불 로직이 추가되어야 함
    }

    @Override
    @Transactional
    public void placeBid(BidHistory bid) {
        bidHistoryRepository.save(bid);
    }

    @Override
    public List<BidHistory> getHistoryByProduct(Long productNo) {
        return bidHistoryRepository.findByProductNoOrderByBidTimeDesc(productNo);
    }
}