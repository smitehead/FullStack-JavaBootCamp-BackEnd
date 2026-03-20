package com.javajava.project.controller;

import com.javajava.project.dto.LoginRequestDto;
import com.javajava.project.dto.LoginResponseDto;
import com.javajava.project.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인
     * POST /api/auth/login
     * 응답: { token, memberNo, userId, nickname }
     * 프론트에서 token을 로컬스토리지 또는 쿠키에 저장 후
     * 이후 요청마다 Authorization: Bearer {token} 헤더에 포함
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginDto) {
        return ResponseEntity.ok(authService.login(loginDto));
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     * JWT는 stateless이므로 서버에서 토큰을 삭제할 수 없음.
     * 프론트에서 저장된 토큰을 삭제하는 방식으로 처리.
     * 서버는 200 OK만 반환.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}
