package com.javajava.project.service;

import com.javajava.project.dto.BillingKeyRegisterRequestDto;
import com.javajava.project.dto.BillingKeyResponseDto;
import com.javajava.project.dto.ChargeRequestDto;
import com.javajava.project.dto.ChargeResponseDto;
import com.javajava.project.dto.PointHistoryResponseDto;

public interface PointService {
    // 카드(빌링키) 등록
    void registerBillingKey(Long memberNo, BillingKeyRegisterRequestDto dto);

    // 등록된 카드 정보 조회
    BillingKeyResponseDto getBillingKey(Long memberNo);

    // 카드 삭제
    void deleteBillingKey(Long memberNo);

    // 포인트 충전
    ChargeResponseDto charge(Long memberNo, ChargeRequestDto dto);

    // 포인트 내역조회
    org.springframework.data.domain.Page<PointHistoryResponseDto> getHistory(Long memberNo, int page, int size);

}
