package com.javajava.project.controller;

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

    @PostMapping("/join")
    public ResponseEntity<Long> join(@RequestBody Member member) {
        return ResponseEntity.ok(memberService.join(member));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberInfo(@PathVariable("id") Long memberNo) {
        return ResponseEntity.ok(memberService.findOne(memberNo));
    }
}