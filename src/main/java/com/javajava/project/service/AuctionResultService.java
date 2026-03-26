package com.javajava.project.service;

import com.javajava.project.dto.AuctionResultResponseDto;

public interface AuctionResultService {

    // 상품 번호로 낙찰 결과 상세 조회 (낙찰자 본인만 가능)
    AuctionResultResponseDto getAuctionResultByProductNo(Long productNo, Long memberNo);

    // 결제 처리 (배송대기 → 결제완료)
    void processPayment(Long resultNo, Long memberNo, String address, String addressDetail);

    // 구매 확정 (결제완료 → 구매확정)
    void confirmPurchase(Long resultNo, Long memberNo);

    // 거래 취소
    void cancelTransaction(Long resultNo, Long memberNo);
}
