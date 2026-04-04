package com.javajava.project.domain.member.service;

import com.javajava.project.global.config.JwtUtil;
import com.javajava.project.domain.member.dto.LoginRequestDto;
import com.javajava.project.domain.member.dto.LoginResponseDto;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.SseService;
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
    private final SseService sseService;

    @Override
    @Transactional
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
        String token = jwtUtil.generateToken(member.getMemberNo(), member.getUserId(), member.getIsAdmin());

        // 6. 동시 로그인 방지: 기존 기기에 SSE로 즉시 강제 로그아웃 이벤트 전송 (연결 중인 경우)
        //    SSE 연결이 없으면 건너뜀 → 이후 API 요청 시 401 인터셉터로 처리됨
        sseService.sendForceLogout(member.getMemberNo());

        // 7. 새로 발급한 토큰을 DB에 저장 (기존 기기의 토큰 자동 무효화)
        member.setCurrentToken(token);

        // 7. 응답 반환 (토큰 + 회원 기본 정보)
        return LoginResponseDto.builder()
                .token(token)
                .memberNo(member.getMemberNo())
                .userId(member.getUserId())
                .nickname(member.getNickname())
                .isAdmin(member.getIsAdmin())
                .build();
    }

    @Override
    @Transactional
    public void logout(Long memberNo) {
        // 로그아웃 시 DB의 currentToken을 제거하여 해당 토큰 즉시 무효화
        memberRepository.findById(memberNo).ifPresent(m -> m.setCurrentToken(null));
    }
}
