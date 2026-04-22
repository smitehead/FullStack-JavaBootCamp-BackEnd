package com.javajava.project.domain.auction.service;

import com.javajava.project.domain.auction.dto.AuctionResultResponseDto;
import com.javajava.project.domain.auction.dto.SellerAuctionResultResponseDto;

public interface AuctionResultService {

    // 상품 번호로 낙찰 결과 상세 조회 (낙찰자 본인만 가능)
    AuctionResultResponseDto getAuctionResultByProductNo(Long productNo, Long memberNo);

    // 상품 번호로 낙찰 결과 조회 (판매자 전용)
    SellerAuctionResultResponseDto getAuctionResultBySellerAndProduct(Long productNo, Long sellerNo);

    // 구매 확정 (배송대기 → 구매확정, 1-step: 에스크로 정산 + 주소 저장 + 확정 통합)
    // 기존 '결제완료' 상태 주문과의 하위 호환을 위해 결제완료 → 구매확정도 처리.
    void confirmPurchase(Long resultNo, Long memberNo, String address, String addressDetail);

    // 강제 승계 낙찰자 단독 취소 (isForcePromoted=1, 패널티 없음)
    void cancelTransaction(Long resultNo, Long memberNo);

    // 일반 낙찰자 취소 요청 (isForcePromoted=0 → 상태: 취소요청, 판매자 동의 필요)
    void requestCancel(Long resultNo, Long memberNo);

    // 판매자 취소 승인 (구매자의 취소요청 승인 — 상태: 취소요청일 때만 허용)
    void approveCancel(Long resultNo, Long sellerNo);

    // 판매자가 취소를 요청 (배송대기 → 판매자취소요청, 구매자 동의 필요)
    void requestCancelBySeller(Long resultNo, Long sellerNo);

    // 구매자가 판매자의 취소 요청을 승인 (판매자취소요청 → 거래취소, 에스크로 환불)
    void approveCancelByBuyer(Long resultNo, Long buyerNo);

    // 판매자가 채팅에서 받은 배송지 주소를 낙찰 결과에 저장
    void updateDeliveryAddress(Long productNo, Long sellerNo, String addrRoad, String addrDetail);
}
