package com.javajava.project.domain.point.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.javajava.project.domain.admin.dto.WithdrawAdminResponseDto;
import com.javajava.project.domain.point.dto.BankAccountDto;
import com.javajava.project.domain.point.dto.BillingKeyRegisterRequestDto;
import com.javajava.project.domain.point.dto.BillingKeyResponseDto;
import com.javajava.project.domain.point.dto.ChargeRequestDto;
import com.javajava.project.domain.point.dto.ChargeResponseDto;
import com.javajava.project.domain.point.dto.PointHistoryResponseDto;
import com.javajava.project.domain.point.dto.WithdrawRequestDto;
import com.javajava.project.domain.point.dto.WithdrawResponseDto;

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

    // 계좌 관련
    List<BankAccountDto> getAccounts(Long memberNo);
    void addAccount(Long memberNo, BankAccountDto dto);
    void deleteAccount(Long memberNo, Long accountNo);

    // 출금 관련
    WithdrawResponseDto withdraw(Long memberNo, WithdrawRequestDto dto);
}
