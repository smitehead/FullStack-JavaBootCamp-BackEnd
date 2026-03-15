package com.javajava.project.service;

import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Product;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidServiceImpl implements BidService {

    private final BidHistoryRepository bidHistoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long placeBid(BidHistory bid) {
        Product product = productRepository.findById(bid.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        // 경매 상태 및 시간 검증 (IS_ACTIVE, END_TIME)
        if (product.getIsActive() == 0 || product.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("종료된 경매입니다.");
        }

        // 최소 입찰 금액 검증 (현재가 + 최소입찰단위)
        long minimumPrice = product.getCurrentPrice() + product.getMinBidUnit();
        if (bid.getBidPrice() < minimumPrice) {
            throw new IllegalArgumentException("최소 입찰가보다 높아야 합니다. 최소가: " + minimumPrice);
        }

        bid.setBidTime(LocalDateTime.now());
        Long savedBidNo = bidHistoryRepository.save(bid).getBidNo();

        // 상품의 현재가(CURRENT_PRICE)와 입찰자 수(BID_COUNT) 업데이트
        product.setCurrentPrice(bid.getBidPrice());
        product.setBidCount(product.getBidCount() + 1);

        return savedBidNo;
    }

    @Override
    public List<BidHistory> getHistoryByProduct(Long productNo) {
        return bidHistoryRepository.findByProductNoOrderByBidTimeDesc(productNo);
    }

    @Override
    @Transactional
    public void cancelBid(Long bidNo, String reason) {
        BidHistory bid = bidHistoryRepository.findById(bidNo)
                .orElseThrow(() -> new IllegalArgumentException("입찰 내역이 없습니다."));
        bid.setIsCancelled(1); // 취소 여부 업데이트
        bid.setCancelReason(reason);
    }
}