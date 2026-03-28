package com.javajava.project.service;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.entity.Member;

public interface MemberService {
    Long join(MemberRequestDto dto);
    Member findOne(Long memberNo);

    // 회원가입 전 프론트에서 실시간 중복 확인용 API
    boolean isUserIdDuplicate(String userId);
    boolean isNicknameDuplicate(String nickname);
    boolean isEmailDuplicate(String email);

    // 프로필 이미지 URL 저장 (/api/images/uuid.jpg 형태의 상대 경로)
    void updateProfileImage(Long memberNo, String url);
}