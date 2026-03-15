package com.javajava.project.controller;

import com.javajava.project.entity.Member;
import com.javajava.project.service.MemberService; // 인터페이스 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    // MemberServiceImpl이 아닌 MemberService 인터페이스를 주입받음
    private final MemberService memberService;

    @PostMapping("/join")
    public ResponseEntity<Long> join(@RequestBody Member member) {
        return ResponseEntity.ok(memberService.join(member));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberInfo(@PathVariable("id") Long memberNo) {
        return ResponseEntity.ok(memberService.findOne(memberNo));
    }
}