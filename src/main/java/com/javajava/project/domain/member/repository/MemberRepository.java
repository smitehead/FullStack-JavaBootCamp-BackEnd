package com.javajava.project.domain.member.repository;

import com.javajava.project.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 관리자용: 활성 회원 목록 (가입일 최신순, 탈퇴 회원 제외)
    List<Member> findByIsActiveOrderByJoinedAtDesc(Integer isActive);

    // 관리자용: 닉네임 또는 이메일로 검색 (탈퇴 회원 제외)
    @Query("SELECT m FROM Member m WHERE m.isActive = 1 AND (LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY m.joinedAt DESC")
    List<Member> searchByKeyword(@Param("keyword") String keyword);

    // 닉네임으로 회원 조회
    Optional<Member> findByNickname(String nickname);
    Optional<Member> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.memberNo = :memberNo")
    Optional<Member> findByIdWithLock(@Param("memberNo") Long memberNo);

    // Oracle 11g는 FETCH FIRST 문법 미지원 → COUNT 기반 커스텀 쿼리로 대체
    // [수정 완료] 탈퇴한 회원(isActive=0)은 중복 검사에서 제외
    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.userId = :userId AND m.isActive = 1")
    boolean existsByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.nickname = :nickname AND m.isActive = 1")
    boolean existsByNickname(@Param("nickname") String nickname);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.email = :email AND m.isActive = 1")
    boolean existsByEmail(@Param("email") String email);

    // 아래 조회용 메서드들도 필요에 따라 isActive=1 조건을 추가하는 것을 권장함
    Optional<Member> findByEmail(String email);

    Optional<Member> findByUserIdAndEmail(String userId, String email);
}