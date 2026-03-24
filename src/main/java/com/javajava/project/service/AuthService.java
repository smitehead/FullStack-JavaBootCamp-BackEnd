package com.javajava.project.service;

import com.javajava.project.dto.LoginRequestDto;
import com.javajava.project.dto.LoginResponseDto;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto dto);
    void logout(Long memberNo);
}
