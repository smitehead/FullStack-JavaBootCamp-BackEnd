package com.javajava.project.service;

import com.javajava.project.dto.BidRequestDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.entity.BidHistory;
import java.util.List;

public interface BidService {
    

    String processBid(BidRequestDto bidDto);


    List<ProductDetailResponseDto.BidHistoryDto> getBidHistory(Long productNo);


    void cancelBid(Long bidNo, String reason);


    void placeBid(BidHistory bid);

    List<BidHistory> getHistoryByProduct(Long productNo);
}