package com.javajava.project.controller;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.entity.Member;
import com.javajava.project.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // RESTful: /join 제거, POST /api/members 자체가 생성을 의미
    @PostMapping
    public ResponseEntity<Long> join(@RequestBody MemberRequestDto memberDto) {
        return ResponseEntity.ok(memberService.join(memberDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberInfo(@PathVariable("id") Long memberNo) {
        return ResponseEntity.ok(memberService.findOne(memberNo));
    }
}