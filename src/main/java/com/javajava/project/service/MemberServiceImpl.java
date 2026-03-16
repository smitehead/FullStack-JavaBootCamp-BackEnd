package com.javajava.project.service;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.entity.Member;
import com.javajava.project.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service //
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService { 

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public Long join(MemberRequestDto dto) {
        // DTO -> Entity 변환 로직
        Member member = Member.builder()
                .userId(dto.getUserId())
                .password(dto.getPassword())
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .phoneNum(dto.getPhoneNum())
                .emdNo(dto.getEmdNo())
                .addrDetail(dto.getAddrDetail())
                .birthDate(dto.getBirthDate())
                .mannerTemp(36.5)
                .points(0L)
                .isActive(1)
                .isAdmin(0)
                .build();

        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getMemberNo();
    }

    private void validateDuplicateMember(Member member) {
        // Repository에 existsByUserId 메서드 구현 필요
        if (memberRepository.findByUserId(member.getUserId()).isPresent()) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
    }

    @Override
    public Member findOne(Long memberNo) {
        return memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }
}