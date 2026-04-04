package com.javajava.project.service;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.dto.MemberResponseDto;
import com.javajava.project.dto.SellerProfileResponseDto;

public interface MemberService {
    Long join(MemberRequestDto dto);
    MemberResponseDto findOne(Long memberNo);

    // 회원가입 전 프론트에서 실시간 중복 확인용 API
    boolean isUserIdDuplicate(String userId);
    boolean isNicknameDuplicate(String nickname);
    boolean isEmailDuplicate(String email);

    // 프로필 이미지 URL 저장 (/api/images/uuid.jpg 형태의 상대 경로)
    void updateProfileImage(Long memberNo, String url);

    // 판매자 프로필 조회 (공개용: 기본 정보 + 판매 상품 목록)
    SellerProfileResponseDto getSellerProfile(Long memberNo);
}