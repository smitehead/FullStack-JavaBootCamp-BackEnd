package com.javajava.project.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터.
 * 모든 HTTP 요청마다 한 번씩 실행 (OncePerRequestFilter).
 *
 * 동작 흐름:
 * 1. Authorization 헤더에서 "Bearer {token}" 추출
 * 2. 토큰 유효성 검증
 * 3. 유효하면 SecurityContext에 인증 정보 등록
 *    → 이후 컨트롤러에서 SecurityContextHolder로 memberNo 조회 가능
 * 4. 토큰 없거나 유효하지 않으면 그냥 통과 (공개 API는 인증 없이 접근 가능)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        // 토큰이 존재하고 유효한 경우에만 SecurityContext에 인증 정보 등록
        if (token != null && jwtUtil.validateToken(token)) {
            Long memberNo = jwtUtil.getMemberNo(token);

            // principal에 memberNo 저장 → 컨트롤러에서 꺼내 쓸 수 있음
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(memberNo, null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 토큰 추출.
     * 형식: "Bearer eyJhbGci..."
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // "Bearer " 이후의 토큰 값만 추출
        }
        return null;
    }
}
