package com.javajava.project.domain.member.dto;

import lombok.*;

@Getter
@Builder
public class LoginResponseDto {

    private String token;      // 발급된 JWT 토큰 (프론트에서 헤더에 담아 사용)
    private Long memberNo;     // 회원번호
    private String userId;     // 아이디
    private String nickname;   // 닉네임 (화면 표시용)
    private Integer isAdmin;   // 관리자 여부 (1: 관리자, 0: 일반 유저)
    private String addrShort;
}
