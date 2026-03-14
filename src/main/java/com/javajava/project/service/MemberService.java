package com.javajava.project.service;


import com.javajava.project.entity.Member;
import com.javajava.project.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Long join(Member member) {
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getMemberNo();
    }

    private void validateDuplicateMember(Member member) {
        if (memberRepository.existsByUserId(member.getUserId())) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
    }

    public Member findOne(Long memberNo) {
        return memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }
}