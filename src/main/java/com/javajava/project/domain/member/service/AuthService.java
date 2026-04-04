package com.javajava.project.domain.member.service;

import com.javajava.project.domain.member.dto.LoginRequestDto;
import com.javajava.project.domain.member.dto.LoginResponseDto;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto dto);
    void logout(Long memberNo);
}
