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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

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

    /**
     * CORS 허용 설정.
     * 프론트엔드가 다른 포트(예: localhost:3000)에서 실행될 때 브라우저의 CORS 차단 방지.
     * allowedOriginPatterns: 허용할 출처 (개발 단계에서는 전체 허용, 운영 시 특정 도메인으로 제한)
     * allowedMethods: 허용할 HTTP 메서드
     * allowedHeaders: 요청에 포함 가능한 헤더 (Authorization 포함)
     * allowCredentials: 쿠키/인증 정보 포함 요청 허용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. CORS 설정
            //    corsConfigurationSource() 빈에서 허용 도메인, 메서드, 헤더를 관리.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 2. CSRF 비활성화
            //    JWT는 stateless 방식이라 세션/쿠키를 사용하지 않으므로 CSRF 불필요.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                .disable()
            )
            // 3. 세션 미사용 (STATELESS)
            //    JWT 방식은 서버가 세션을 유지하지 않음.
            //    요청마다 토큰으로만 인증.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 4. 요청 권한 설정
            //    현재는 개발 단계이므로 전체 허용.
            //    추후 관리자 페이지 등은 .hasRole("ADMIN") 등으로 세분화 가능.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                .anyRequest().permitAll()
            )
            // 5. H2 콘솔 iframe 접근 허용
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // 6. JWT 필터 등록
            //    Spring Security의 기본 로그인 필터(UsernamePasswordAuthenticationFilter) 앞에
            //    JWT 검증 필터를 삽입 → 토큰이 있으면 SecurityContext에 인증 정보 등록
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
