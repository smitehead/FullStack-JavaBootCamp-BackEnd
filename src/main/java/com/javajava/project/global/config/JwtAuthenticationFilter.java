package com.javajava.project.global.config;

import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * JWT 인증 필터.
 * 모든 HTTP 요청마다 한 번씩 실행 (OncePerRequestFilter).
 *
 * 동작 흐름:
 * 1. Authorization 헤더에서 "Bearer {token}" 추출
 * 2. 토큰 유효성 검증 (서명/만료)
 * 3. DB에 저장된 currentToken과 비교 (동시 로그인 방지)
 *    - 불일치 시 401 반환 → 프론트에서 자동 로그아웃 처리
 * 4. 유효하면 SecurityContext에 인증 정보 등록
 * 5. 토큰 없거나 유효하지 않으면 그냥 통과 (공개 API는 인증 없이 접근 가능)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        // 토큰이 존재하고 유효한 경우에만 SecurityContext에 인증 정보 등록
        if (token != null && jwtUtil.validateToken(token)) {
            Long memberNo = jwtUtil.getMemberNo(token);

            // 동시 로그인 방지: DB에 저장된 currentToken과 비교
            Optional<Member> memberOpt = memberRepository.findById(memberNo);
            if (memberOpt.isPresent()) {
                String currentToken = memberOpt.get().getCurrentToken();
                if (currentToken != null && !token.equals(currentToken)) {
                    // 다른 기기에서 새로 로그인하여 토큰이 교체된 경우 → 401 반환
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"다른 기기에서 로그인되어 자동 로그아웃 처리되었습니다.\"}");
                    return;
                }
            }

            // isAdmin 값으로 권한 설정
            int isAdmin = jwtUtil.getIsAdmin(token);
            List<SimpleGrantedAuthority> authorities = (isAdmin == 1)
                    ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    : List.of(new SimpleGrantedAuthority("ROLE_USER"));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(memberNo, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
