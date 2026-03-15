package com.javajava.project.service;

import com.javajava.project.entity.BidHistory;
import java.util.List;

public interface BidService {
    Long placeBid(BidHistory bid);
    List<BidHistory> getHistoryByProduct(Long productNo);
    void cancelBid(Long bidNo, String reason);
}