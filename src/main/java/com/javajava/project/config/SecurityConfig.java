package com.javajava.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 비밀번호 단방향 암호화에 사용할 BCrypt 인코더 빈 등록.
     * BCrypt는 같은 문자열도 매번 다른 해시값을 생성하며 레인보우 테이블 공격에 강함.
     * MemberServiceImpl에서 생성자 주입으로 받아 사용됨.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF 비활성화
            //    REST API는 세션 대신 토큰 방식을 사용하므로 CSRF 불필요.
            //    H2 콘솔은 폼 기반이라 개별적으로 ignoringRequestMatchers 처리.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                .disable()
            )
            // 2. 모든 요청 허용 (개발/테스트 단계)
            //    추후 인증 기능 구현 시 .authenticated() 등으로 세분화 필요.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                .anyRequest().permitAll()
            )
            // 3. H2 콘솔 iframe 접근 허용
            //    Spring Security 기본값은 모든 iframe을 차단하므로
            //    같은 출처(sameOrigin)에서는 허용하도록 완화.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}