package com.javajava.project.controller;

import com.javajava.project.dto.LoginRequestDto;
import com.javajava.project.dto.LoginResponseDto;
import com.javajava.project.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
     * DB에서 currentToken을 삭제하여 해당 토큰을 즉시 무효화.
     * 프론트에서도 로컬스토리지의 토큰을 삭제.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // SecurityContext에서 현재 로그인한 회원 번호 추출
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof Long memberNo) {
            authService.logout(memberNo);
        }
        return ResponseEntity.ok().build();
    }
}
