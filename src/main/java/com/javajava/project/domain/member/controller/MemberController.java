package com.javajava.project.domain.member.controller;

import com.javajava.project.domain.member.dto.*;
import com.javajava.project.domain.member.entity.BlockedUser;
import com.javajava.project.domain.member.entity.BlockedUserId;
import com.javajava.project.domain.member.repository.BlockedUserRepository;
import com.javajava.project.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final BlockedUserRepository blockedUserRepository;

    private Long getCurrentMemberNo() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** 차단 */
    @PostMapping("/me/blocked/{targetMemberNo}")
    public ResponseEntity<Void> blockUser(@PathVariable Long targetMemberNo) {
        Long memberNo = getCurrentMemberNo();
        if (memberNo.equals(targetMemberNo))
            throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다.");

        BlockedUserId id = new BlockedUserId(memberNo, targetMemberNo);
        if (!blockedUserRepository.existsById(id)) {
            blockedUserRepository.save(BlockedUser.builder().id(id).build());
        }
        return ResponseEntity.ok().build();
    }


    /** 차단 여부 조회 */
    @GetMapping("/me/blocked/{targetMemberNo}")
    public ResponseEntity<Map<String, Boolean>> isBlocked(@PathVariable Long targetMemberNo) {
        Long memberNo = getCurrentMemberNo();
        boolean blocked = blockedUserRepository.existsById(new BlockedUserId(memberNo, targetMemberNo));
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }


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
    public ResponseEntity<MemberResponseDto> getMemberInfo(@PathVariable("id") Long memberNo) {
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

    /**
     * 프로필 이미지 URL 저장
     * PUT /api/members/{memberNo}/profile-image-url
     * 파일 업로드는 POST /api/images/upload 로 먼저 완료 후, 반환된 URL을 여기에 저장
     * 요청: { "url": "/api/images/uuid.jpg" }
     */
    /**
     * 판매자 공개 프로필 조회
     * GET /api/members/{memberNo}/seller-profile
     * 응답: 판매자 기본 정보 + 판매 상품 목록
     */
    @GetMapping("/{memberNo}/seller-profile")
    public ResponseEntity<SellerProfileResponseDto> getSellerProfile(
            @PathVariable("memberNo") Long memberNo) {
        Long viewerNo = null;
        try {
            viewerNo = getCurrentMemberNo();
        } catch (Exception e) {
            // 로그인하지 않은 경우 (Anonymous)
        }
        return ResponseEntity.ok(memberService.getSellerProfile(memberNo, viewerNo));
    }

    @PutMapping("/{memberNo}/profile-image-url")
    public ResponseEntity<Void> updateProfileImageUrl(
            @PathVariable("memberNo") Long memberNo,
            @RequestBody Map<String, String> body) {
        memberService.updateProfileImage(memberNo, body.get("url"));
        return ResponseEntity.ok().build();
    }

    /** 내 프로필 조회 */
    @GetMapping("/me")
    public ResponseEntity<MemberProfileResponseDto> getProfile() {
        return ResponseEntity.ok(memberService.getProfile(getCurrentMemberNo()));
    }

    /** 프로필 수정 */
    @PutMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(@Valid @RequestBody MemberProfileUpdateDto dto) {
        memberService.updateProfile(getCurrentMemberNo(), dto);
        return ResponseEntity.ok().build();
    }

    /** 이메일 수정 */
    @PutMapping("/me/email")
    public ResponseEntity<Void> updateEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        memberService.updateEmail(getCurrentMemberNo(), email);
        return ResponseEntity.ok().build();
    }

    /** 비밀번호 변경 */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeDto dto) {
        memberService.changePassword(getCurrentMemberNo(), dto);
        return ResponseEntity.ok().build();
    }

    /** 알림 설정 변경 */
    @PutMapping("/me/notification")
    public ResponseEntity<Void> updateNotification(@RequestBody NotificationSettingDto dto) {
        memberService.updateNotificationSetting(getCurrentMemberNo(), dto);
        return ResponseEntity.ok().build();
    }

    /** 회원 탈퇴 */
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@Valid @RequestBody WithdrawMemberDto dto) {
        Long memberNo = getCurrentMemberNo();
        memberService.withdraw(memberNo, dto);
        // 탈퇴 후 토큰 무효화 응답
        return ResponseEntity.ok().build();
    }

    /** 차단 사용자 목록 */
    @GetMapping("/me/blocked")
    public ResponseEntity<List<BlockedUserResponseDto>> getBlockedUsers() {
        return ResponseEntity.ok(memberService.getBlockedUsers(getCurrentMemberNo()));
    }

    /** 차단 해제 */
    @DeleteMapping("/me/blocked/{targetMemberNo}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long targetMemberNo) {
        memberService.unblockUser(getCurrentMemberNo(), targetMemberNo);
        return ResponseEntity.ok().build();
    }

}