package com.javajava.project.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티.
 * - 로그인 성공 시 토큰 발급 (generateToken)
 * - 매 요청마다 토큰 유효성 검증 (validateToken)
 * - 토큰에서 회원 정보 추출 (getMemberNo, getUserId)
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        // application.properties의 jwt.secret 값으로 서명 키 생성
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * JWT 토큰 생성.
     * @param memberNo 회원번호 (subject로 저장)
     * @param userId   아이디 (추가 claim으로 저장)
     */
    public String generateToken(Long memberNo, String userId, Integer isAdmin) {
        return Jwts.builder()
                .subject(String.valueOf(memberNo))
                .claim("userId", userId)
                .claim("isAdmin", isAdmin)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public int getIsAdmin(String token) {
        return getClaims(token).get("isAdmin", Integer.class);
    }

    /**
     * 토큰에서 Claims(페이로드) 추출.
     * 서명 검증 실패 또는 만료 시 JwtException 발생.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰에서 회원번호 추출
    public Long getMemberNo(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    // 토큰에서 아이디 추출
    public String getUserId(String token) {
        return getClaims(token).get("userId", String.class);
    }

    /**
     * 토큰 유효성 검증.
     * 만료, 위변조, 형식 오류 등 모든 JWT 예외를 false로 처리.
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
