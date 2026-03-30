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
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://54.164.62.214:3000",
                "http://54.164.62.214:5173",
                "http://54.164.62.214"
        ));
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
            //    위의 corsConfigurationSource() 빈을 필터 체인에 연결.
            //    React ↔ Spring Boot 간 다른 포트 통신 허용.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. CSRF 비활성화
            //    CSRF란 악성 사이트가 로그인한 사용자인 척 요청을 위조하는 공격.
            //    쿠키는 브라우저가 자동으로 붙여서 위조 가능하지만,
            //    JWT는 프론트가 직접 Authorization 헤더에 붙여야 해서 위조 불가.
            //    따라서 JWT 방식에서는 CSRF 보호가 불필요해 비활성화.
            .csrf(csrf -> csrf.disable())

            // 3. 세션 미사용 (STATELESS)
            //    JWT 방식은 서버가 세션을 저장하지 않음.
            //    로그인 상태를 서버가 기억하는 대신, 매 요청마다 토큰으로 인증.
            //    서버를 여러 대 운영해도 세션 공유 문제가 없음.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 4. 요청 권한 설정
            //    현재 개발 단계로 전체 허용.
            //    관리자 기능 구현 시 아래처럼 세분화 예정:
            //    .requestMatchers("/api/admin/**").hasRole("ADMIN")
            //    → Member 엔티티에 role 필드 추가 + JWT 토큰에 role 포함 필요
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )

            // 5. JWT 필터 등록
            //    Spring Security 기본 로그인 필터(UsernamePasswordAuthenticationFilter) 앞에 삽입.
            //    요청이 들어오면 JWT 필터가 먼저 실행됨:
            //      Authorization 헤더에서 "Bearer " 제거 → 토큰 추출 → 검증 → SecurityContext 등록
            //    토큰 없으면 그냥 통과 (인증 안 된 상태로 진행, 권한 설정에서 걸림)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
