package com.javajava.project.domain.bid.service;

import com.javajava.project.domain.bid.dto.BidRequestDto;
import com.javajava.project.domain.product.dto.ProductDetailResponseDto;
import com.javajava.project.domain.bid.entity.BidHistory;
import java.util.List;

public interface BidService {
    

    String processBid(BidRequestDto bidDto);


    List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo);


    void cancelBid(Long bidNo, String reason);


    void placeBid(BidHistory bid);

    List<BidHistory> getHistoryByProduct(Long productNo);
}