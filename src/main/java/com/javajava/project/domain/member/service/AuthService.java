package com.javajava.project.domain.member.service;

import com.javajava.project.domain.member.dto.LoginRequestDto;
import com.javajava.project.domain.member.dto.LoginResponseDto;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto dto);
    void logout(Long memberNo);
    String findIdByEmail(String email);
    void sendResetCode(String userId, String email) throws jakarta.mail.MessagingException;
    void resetPassword(String userId, String email) throws jakarta.mail.MessagingException;
}
