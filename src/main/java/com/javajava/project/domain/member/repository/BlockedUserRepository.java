package com.javajava.project.domain.member.repository;

import com.javajava.project.domain.member.entity.BlockedUser;
import com.javajava.project.domain.member.entity.BlockedUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, BlockedUserId> {

    // 특정 회원이 차단한 목록 조회
    List<BlockedUser> findByIdMemberNo(Long memberNo);

    // 특정 회원이 특정 대상을 차단했는지 여부 확인
    boolean existsByIdMemberNoAndIdBlockedMemberNo(Long memberNo, Long blockedMemberNo);

    // 차단 해제
    void deleteByIdMemberNoAndIdBlockedMemberNo(Long memberNo, Long blockedMemberNo);

    // 나를 차단한 회원 목록 조회 (채팅방 접근 제어 등에 활용)
    List<BlockedUser> findByIdBlockedMemberNo(Long blockedMemberNo);
}
