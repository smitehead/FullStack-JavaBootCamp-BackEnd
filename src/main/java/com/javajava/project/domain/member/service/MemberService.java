package com.javajava.project.domain.member.service;

import com.javajava.project.domain.member.dto.MemberProfileResponseDto;
import com.javajava.project.domain.member.dto.BlockedUserResponseDto;
import com.javajava.project.domain.member.dto.MemberProfileUpdateDto;
import com.javajava.project.domain.member.dto.MemberRequestDto;
import com.javajava.project.domain.member.dto.MemberResponseDto;
import com.javajava.project.domain.member.dto.NotificationSettingDto;
import com.javajava.project.domain.member.dto.PasswordChangeDto;
import com.javajava.project.domain.member.dto.SellerProfileResponseDto;
import com.javajava.project.domain.member.dto.WithdrawMemberDto;

import java.util.List;

public interface MemberService {
    Long join(MemberRequestDto dto);
    MemberResponseDto findOne(Long memberNo);
    
    void updateEmail(Long memberNo, String email);

    // 회원가입 전 프론트에서 실시간 중복 확인용 API
    boolean isUserIdDuplicate(String userId);
    boolean isNicknameDuplicate(String nickname);
    boolean isEmailDuplicate(String email);

    // 프로필 이미지 URL 저장 (/api/images/uuid.jpg 형태의 상대 경로)
    void updateProfileImage(Long memberNo, String url);

    // 판매자 프로필 조회 (공개용: 기본 정보 + 판매 상품 목록)
    SellerProfileResponseDto getSellerProfile(Long memberNo, Long viewerNo);

    void updateProfile(Long memberNo, MemberProfileUpdateDto dto);
    void changePassword(Long memberNo, PasswordChangeDto dto);
    void updateNotificationSetting(Long memberNo, NotificationSettingDto dto);
    void withdraw(Long memberNo, WithdrawMemberDto dto);
    MemberProfileResponseDto getProfile(Long memberNo);

    List<BlockedUserResponseDto> getBlockedUsers(Long memberNo);
    void blockUser(Long memberNo, Long targetMemberNo);
    void unblockUser(Long memberNo, Long targetMemberNo);
}