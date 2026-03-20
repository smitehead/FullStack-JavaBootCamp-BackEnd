package com.javajava.project.controller;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.entity.Member;
import com.javajava.project.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입
     * POST /api/members
     * @Valid: MemberRequestDto의 Bean Validation 어노테이션을 실행.
     *         검증 실패 시 MethodArgumentNotValidException 발생 → GlobalExceptionHandler가 400으로 처리.
     */
    @PostMapping
    public ResponseEntity<Long> join(@Valid @RequestBody MemberRequestDto memberDto) {
        return ResponseEntity.ok(memberService.join(memberDto));
    }

    // 회원 단건 조회
    // GET /api/members/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberInfo(@PathVariable("id") Long memberNo) {
        return ResponseEntity.ok(memberService.findOne(memberNo));
    }

    // ---- 실시간 중복 확인 API ----
    // 프론트에서 입력 완료 시 Ajax로 호출하여 { "duplicate": true/false } 반환

    // GET /api/members/check-userid?userId=xxx
    @GetMapping("/check-userid")
    public ResponseEntity<Map<String, Boolean>> checkUserId(@RequestParam String userId) {
        return ResponseEntity.ok(Map.of("duplicate", memberService.isUserIdDuplicate(userId)));
    }

    // GET /api/members/check-nickname?nickname=xxx
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(Map.of("duplicate", memberService.isNicknameDuplicate(nickname)));
    }

    // GET /api/members/check-email?email=xxx
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("duplicate", memberService.isEmailDuplicate(email)));
    }
}