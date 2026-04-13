package com.javajava.project.domain.bid.service;

import com.javajava.project.domain.bid.dto.AutoBidRequestDto;
import com.javajava.project.domain.bid.dto.AutoBidResponseDto;

import java.util.Optional;

public interface AutoBidService {

    /** 자동입찰 등록 (이미 있으면 maxPrice 갱신) */
    AutoBidResponseDto registerAutoBid(Long memberNo, AutoBidRequestDto dto);

    /** 자동입찰 취소 */
    void cancelAutoBid(Long memberNo, Long productNo);

    /**
     * 입찰 발생 시 자동입찰 트리거
     * @return true = 자동입찰이 실행되어 SSE를 전송했음, false = 자동입찰 없음/불가
     */
    boolean triggerAutoBids(Long productNo, Long currentPrice, Long triggerMemberNo);

    /** 특정 회원의 특정 상품 활성 자동입찰 조회 */
    Optional<AutoBidResponseDto> getActiveAutoBid(Long memberNo, Long productNo);
}
