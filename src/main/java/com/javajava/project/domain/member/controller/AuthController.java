package com.javajava.project.domain.member.controller;

import com.javajava.project.domain.member.dto.LoginRequestDto;
import com.javajava.project.domain.member.dto.LoginResponseDto;
import com.javajava.project.domain.member.service.AuthService;
import com.javajava.project.domain.member.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

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

    /**
     * 이메일 인증번호 발송
     * POST /api/auth/send-email-code
     * 요청: { "email": "user@example.com" }
     */
    @PostMapping("/send-email-code")
    public ResponseEntity<Void> sendEmailCode(@RequestBody Map<String, String> body) throws MessagingException {
        String email = body.get("email");
        emailService.sendVerificationCode(email);
        return ResponseEntity.ok().build();
    }

    /**
     * 이메일 인증번호 검증
     * POST /api/auth/verify-email-code
     * 요청: { "email": "user@example.com", "code": "123456" }
     * 응답: { "verified": true/false }
     */
    @PostMapping("/verify-email-code")
    public ResponseEntity<Map<String, Boolean>> verifyEmailCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        boolean verified = emailService.verifyCode(email, code);
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    /**
     * 아이디 찾기
     * POST /api/auth/find-id
     * 요청: { "email": "user@example.com" }
     * 응답: { "userId": "testuser" }
     */
    @PostMapping("/find-id")
    public ResponseEntity<Map<String, String>> findId(@RequestBody Map<String, String> body) {
        String userId = authService.findIdByEmail(body.get("email"));
        return ResponseEntity.ok(Map.of("userId", userId));
    }

    /**
     * 비밀번호 재설정용 인증번호 발송 (아이디+이메일 일치 여부 검증 포함)
     * POST /api/auth/send-reset-code
     * 요청: { "userId": "testuser", "email": "user@example.com" }
     */
    @PostMapping("/send-reset-code")
    public ResponseEntity<Void> sendResetCode(@RequestBody Map<String, String> body) throws MessagingException {
        authService.sendResetCode(body.get("userId"), body.get("email"));
        return ResponseEntity.ok().build();
    }

    /**
     * 임시 비밀번호 발급 및 이메일 발송
     * POST /api/auth/reset-pw
     * 요청: { "userId": "testuser", "email": "user@example.com" }
     */
    @PostMapping("/reset-pw")
    public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> body) throws MessagingException {
        authService.resetPassword(body.get("userId"), body.get("email"));
        return ResponseEntity.ok().build();
    }
}
