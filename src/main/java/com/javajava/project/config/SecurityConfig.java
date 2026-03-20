package com.javajava.project.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 비밀번호 단방향 암호화에 사용할 BCrypt 인코더 빈 등록.
     * BCrypt는 같은 문자열도 매번 다른 해시값을 생성하며 레인보우 테이블 공격에 강함.
     * MemberServiceImpl, AuthServiceImpl에서 생성자 주입으로 받아 사용됨.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF 비활성화
            //    JWT는 stateless 방식이라 세션/쿠키를 사용하지 않으므로 CSRF 불필요.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                .disable()
            )
            // 2. 세션 미사용 (STATELESS)
            //    JWT 방식은 서버가 세션을 유지하지 않음.
            //    요청마다 토큰으로만 인증.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 3. 요청 권한 설정
            //    현재는 개발 단계이므로 전체 허용.
            //    추후 관리자 페이지 등은 .hasRole("ADMIN") 등으로 세분화 가능.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                .anyRequest().permitAll()
            )
            // 4. H2 콘솔 iframe 접근 허용
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // 5. JWT 필터 등록
            //    Spring Security의 기본 로그인 필터(UsernamePasswordAuthenticationFilter) 앞에
            //    JWT 검증 필터를 삽입 → 토큰이 있으면 SecurityContext에 인증 정보 등록
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}