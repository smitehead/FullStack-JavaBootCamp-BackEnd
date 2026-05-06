package com.javajava.project.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

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
     * CORS(Cross-Origin Resource Sharing) 허용 설정.
     *
     * 브라우저는 기본적으로 다른 출처(도메인/포트)로의 요청을 차단함.
     * React(localhost:3000) → Spring Boot(localhost:8080) 처럼 포트가 다르면 CORS 오류 발생.
     * 이 설정으로 서버에서 허용 범위를 지정해 차단을 해제함.
     *
     * - allowedOriginPatterns: 허용할 출처 (* = 전체, 운영 시 실제 도메인으로 교체 필요)
     * - allowedMethods: 허용할 HTTP 메서드 (OPTIONS는 브라우저 preflight 요청용)
     * - allowedHeaders: 허용할 요청 헤더 (* = 전체, Authorization 헤더 포함)
     * - allowCredentials: 쿠키/인증 정보 포함 요청 허용
     *
     * ※ CORS는 도메인 차단만 제어하며, 관리자/일반유저 권한 구분은 JWT + hasRole()로 별도 처리해야 함.
     * ※ Thunder Client 같은 API 테스트 도구는 브라우저가 아니라 CORS 영향 없음.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
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
                // 위의 corsConfigurationSource() 빈을 필터 체인에 연결.
                // React ↔ Spring Boot 간 다른 포트 통신 허용.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. CSRF 비활성화
                // CSRF란 악성 사이트가 로그인한 사용자인 척 요청을 위조하는 공격.
                // 쿠키는 브라우저가 자동으로 붙여서 위조 가능하지만,
                // JWT는 프론트가 직접 Authorization 헤더에 붙여야 해서 위조 불가.
                // 따라서 JWT 방식에서는 CSRF 보호가 불필요해 비활성화.
                .csrf(csrf -> csrf.disable())

                // 3. 세션 미사용 (STATELESS)
                // JWT 방식은 서버가 세션을 저장하지 않음.
                // 로그인 상태를 서버가 기억하는 대신, 매 요청마다 토큰으로 인증.
                // 서버를 여러 대 운영해도 세션 공유 문제가 없음.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 불필요 (공개 API)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/sse/**").permitAll()
                        .requestMatchers("/api/images/**").permitAll()
                        .requestMatchers("/api/banners/**").permitAll()
                        .requestMatchers("/ws-stomp/**").permitAll()

                        // Members: /me/** 전체 인증 필수, 프로필 이미지 변경 인증 필수, 나머지 공개
                        .requestMatchers("/api/members/me/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/members/*/profile-image-url").authenticated()
                        .requestMatchers("/api/members/**").permitAll()

                        // Products: 내 목록·등록은 인증 필수
                        .requestMatchers("/api/products/my-selling").authenticated()
                        .requestMatchers("/api/products/my-bidding").authenticated()
                        .requestMatchers("/api/products/my-purchased").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products").authenticated()
                        // QnA: 목록 조회는 공개, 작성/답변/삭제는 인증 필수
                        .requestMatchers(HttpMethod.GET, "/api/products/*/qna").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/qna/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/products/*/qna/**").authenticated()
                        .requestMatchers("/api/products/**").permitAll()

                        // Bids: 입찰 기록 조회는 공개, 입찰 취소(관리자), 나머지 인증 필수
                        .requestMatchers(HttpMethod.GET, "/api/bids/product/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/bids/*/cancel").hasRole("ADMIN")
                        .requestMatchers("/api/bids/**").authenticated()

                        // 자동입찰·위시리스트·알림·낙찰결과: 전체 인증 필수
                        .requestMatchers("/api/auto-bid/**").authenticated()
                        .requestMatchers("/api/wishlists/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/auction-results/**").authenticated()

                        // Reviews: 받은 리뷰 목록·태그 목록은 공개, 나머지 인증 필수
                        .requestMatchers(HttpMethod.GET, "/api/reviews/target/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/tags").permitAll()
                        .requestMatchers("/api/reviews/**").authenticated()

                        // Chat·Inquiries·Reports·Points: 전체 인증 필수
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/inquiries/**").authenticated()
                        .requestMatchers("/api/reports/**").authenticated()
                        .requestMatchers("/api/points/**").authenticated()

                        // Notices: 관리자 전용 쓰기, /all, 나머지 GET은 공개
                        .requestMatchers(HttpMethod.POST, "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers("/api/notices/all").hasRole("ADMIN")
                        .requestMatchers("/api/notices/**").permitAll()

                        // Admin: 관리자 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 명시되지 않은 모든 요청은 인증 필수 (화이트리스트 방식)
                        .anyRequest().authenticated())

                // 4-1. 보안 헤더 설정
                // X-Frame-Options: 클릭재킹 방지 (iframe 삽입 차단)
                // X-Content-Type-Options: MIME 스니핑 방지 (브라우저가 Content-Type 무시하는 것 차단)
                // Content-Security-Policy: XSS 방지 - 허용된 출처의 스크립트만 실행
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(cto -> {})
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self'; " +
                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                "font-src 'self' https://fonts.gstatic.com; " +
                                "img-src 'self' data: blob:; " +
                                "connect-src 'self' ws: wss:; " +
                                "frame-ancestors 'none'"
                        ))
                )

                // 5. JWT 필터 등록
                // Spring Security 기본 로그인 필터(UsernamePasswordAuthenticationFilter) 앞에 삽입.
                // 요청이 들어오면 JWT 필터가 먼저 실행됨:
                // Authorization 헤더에서 "Bearer " 제거 → 토큰 추출 → 검증 → SecurityContext 등록
                // 토큰 없으면 그냥 통과 (인증 안 된 상태로 진행, 권한 설정에서 걸림)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
