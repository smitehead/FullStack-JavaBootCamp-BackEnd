package com.javajava.project.domain.auction.service;

import com.javajava.project.domain.auction.dto.AuctionResultResponseDto;

public interface AuctionResultService {

    // 상품 번호로 낙찰 결과 상세 조회 (낙찰자 본인만 가능)
    AuctionResultResponseDto getAuctionResultByProductNo(Long productNo, Long memberNo);

    // 구매 확정 (배송대기 → 구매확정, 1-step: 에스크로 정산 + 주소 저장 + 확정 통합)
    // 기존 '결제완료' 상태 주문과의 하위 호환을 위해 결제완료 → 구매확정도 처리.
    void confirmPurchase(Long resultNo, Long memberNo, String address, String addressDetail);

    // 거래 취소
    void cancelTransaction(Long resultNo, Long memberNo);
}
