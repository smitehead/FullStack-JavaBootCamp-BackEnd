package com.javajava.project.domain.bid.service;

import com.javajava.project.domain.bid.dto.BidRequestDto;
import com.javajava.project.domain.bid.dto.BidResultDto;
import com.javajava.project.domain.product.dto.ProductDetailResponseDto;
import com.javajava.project.domain.bid.entity.BidHistory;
import java.util.List;

public interface BidService {

    /** 입찰 처리. 오류 시 IllegalArgumentException/IllegalStateException 발생 */
    BidResultDto processBid(BidRequestDto bidDto);

    /** 즉시구매 처리 (비관적 락 + 경매 즉시 종료) */
    String processBuyout(Long productNo, Long memberNo);


    List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo);


    void cancelBid(Long bidNo, String reason);


    void placeBid(BidHistory bid);

    List<BidHistory> getHistoryByProduct(Long productNo);
}