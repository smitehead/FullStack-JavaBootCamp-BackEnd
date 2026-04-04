package com.javajava.project.domain.member.repository;

import com.javajava.project.domain.member.entity.BlockedUser;
import com.javajava.project.domain.member.entity.BlockedUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, BlockedUserId> {

    // 특정 회원이 차단한 목록 조회
    List<BlockedUser> findByMemberNo(Long memberNo);

    // 특정 회원이 특정 대상을 차단했는지 여부 확인
    @Query("SELECT COUNT(b) > 0 FROM BlockedUser b WHERE b.memberNo = :memberNo AND b.blockedMemberNo = :blockedMemberNo")
    boolean existsByMemberNoAndBlockedMemberNo(@Param("memberNo") Long memberNo,
                                               @Param("blockedMemberNo") Long blockedMemberNo);

    // 나를 차단한 회원 목록 조회 (채팅방 접근 제어 등에 활용)
    List<BlockedUser> findByBlockedMemberNo(Long blockedMemberNo);
}
