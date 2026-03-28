package com.javajava.project.service;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.entity.Member;
import com.javajava.project.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
// 기본적으로 조회 트랜잭션 (readOnly = true) → 불필요한 변경 감지 스냅샷 생략으로 성능 향상
// 데이터를 변경하는 메서드에는 별도로 @Transactional 을 붙여 readOnly를 덮어씀
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig에서 빈으로 등록된 BCryptPasswordEncoder

    @Override
    @Transactional // 쓰기 작업이므로 readOnly 덮어씀
    public Long join(MemberRequestDto dto) {
        // 1단계: 아이디·닉네임·이메일 중복 여부 검증 (중복 시 IllegalStateException → 409 응답)
        validateDuplicate(dto);

        // 2단계: 만 14세 미만 가입 제한 (ERD 정책)
        //        birthDate + 14년 이 오늘보다 미래이면 아직 14세가 안 된 것
        if (dto.getBirthDate().plusYears(14).isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("만 14세 미만은 가입할 수 없습니다.");
        }

        // 3단계: DTO → Entity 변환
        //        비밀번호는 반드시 BCrypt로 암호화하여 저장 (평문 저장 금지)
        Member member = Member.builder()
                .userId(dto.getUserId())
                .password(passwordEncoder.encode(dto.getPassword())) // BCrypt 단방향 암호화
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .phoneNum(dto.getPhoneNum())
                .emdNo(dto.getEmdNo())
                .addrDetail(dto.getAddrDetail())
                .birthDate(dto.getBirthDate())
                // marketingAgree는 선택값이므로 null이면 기본값 0(미동의) 처리
                .marketingAgree(dto.getMarketingAgree() != null ? dto.getMarketingAgree() : 0)
                .build();

        memberRepository.save(member);
        return member.getMemberNo(); // 저장 후 생성된 PK 반환
    }

    /**
     * 아이디, 닉네임, 이메일 중복 검증.
     * 하나라도 중복이면 IllegalStateException 발생 → GlobalExceptionHandler가 409로 처리.
     */
    private void validateDuplicate(MemberRequestDto dto) {
        if (memberRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
        if (memberRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalStateException("이미 존재하는 닉네임입니다.");
        }
        if (memberRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }
    }

    @Override
    public Member findOne(Long memberNo) {
        return memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    // ---- 실시간 중복 확인 (프론트에서 입력 중 Ajax 호출용) ----

    @Override
    public boolean isUserIdDuplicate(String userId) {
        return memberRepository.existsByUserId(userId);
    }

    @Override
    public boolean isNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Override
    public boolean isEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updateProfileImage(Long memberNo, String url) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.setProfileImgUrl(url);
    }
}