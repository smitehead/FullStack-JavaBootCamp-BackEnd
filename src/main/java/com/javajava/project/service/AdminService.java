package com.javajava.project.service;

import com.javajava.project.dto.AdminMemberResponseDto;
import com.javajava.project.dto.MannerHistoryResponseDto;

import java.util.List;

public interface AdminService {

    // 회원 목록 조회 (전체)
    List<AdminMemberResponseDto> getAllMembers();

    // 회원 검색 (닉네임/이메일)
    List<AdminMemberResponseDto> searchMembers(String keyword);

    // 회원 정지
    void suspendMember(Long memberNo, Integer suspendDays, String suspendReason, Long adminNo);

    // 회원 정지 해제
    void unsuspendMember(Long memberNo, Long adminNo);

    // 매너온도 변경
    void updateMannerTemp(Long memberNo, Double newTemp, String reason, Long adminNo);

    // 포인트 변경
    void updatePoints(Long memberNo, Long pointAmount, Long adminNo);

    // 권한 변경
    void updateRole(Long memberNo, Integer isAdmin, Long adminNo);

    // 매너온도 변동 이력 조회 (전체)
    List<MannerHistoryResponseDto> getAllMannerHistory();

    // 매너온도 변동 이력 조회 (특정 회원)
    List<MannerHistoryResponseDto> getMannerHistoryByMember(Long memberNo);
}
