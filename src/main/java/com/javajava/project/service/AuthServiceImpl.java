package com.javajava.project.service;

import com.javajava.project.config.JwtUtil;
import com.javajava.project.dto.LoginRequestDto;
import com.javajava.project.dto.LoginResponseDto;
import com.javajava.project.entity.Member;
import com.javajava.project.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponseDto login(LoginRequestDto dto) {
        // 1. 아이디로 회원 조회 (없으면 예외)
        Member member = memberRepository.findByUserId(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 잘못되었습니다."));

        // 2. 계정 활성 상태 확인 (탈퇴 회원 로그인 차단)
        if (member.getIsActive() == 0) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }

        // 3. 정지 상태 확인
        if (member.getIsSuspended() == 1) {
            throw new IllegalArgumentException("정지된 계정입니다. 관리자에게 문의하세요.");
        }

        // 4. 비밀번호 검증 (BCrypt 해시 비교)
        //    입력된 평문 password와 DB에 저장된 BCrypt 해시값을 비교
        if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 잘못되었습니다.");
        }

        // 5. JWT 토큰 발급
        String token = jwtUtil.generateToken(member.getMemberNo(), member.getUserId());

        // 6. 응답 반환 (토큰 + 회원 기본 정보)
        return LoginResponseDto.builder()
                .token(token)
                .memberNo(member.getMemberNo())
                .userId(member.getUserId())
                .nickname(member.getNickname())
                .build();
    }
}
