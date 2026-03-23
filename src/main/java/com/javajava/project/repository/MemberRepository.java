package com.javajava.project.repository;

import com.javajava.project.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.memberNo = :memberNo")
    Optional<Member> findByIdWithLock(@Param("memberNo") Long memberNo);

    // Oracle 11g는 FETCH FIRST 문법 미지원 → COUNT 기반 커스텀 쿼리로 대체
    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.userId = :userId")
    boolean existsByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.nickname = :nickname")
    boolean existsByNickname(@Param("nickname") String nickname);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.email = :email")
    boolean existsByEmail(@Param("email") String email);
}