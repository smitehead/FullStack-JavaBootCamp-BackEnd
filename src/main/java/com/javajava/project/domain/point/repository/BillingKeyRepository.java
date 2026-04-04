package com.javajava.project.domain.point.repository;

import com.javajava.project.domain.point.entity.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
    // 회원의 빌링키 조회 (카드등록 여부확인 및 충전시 customerUid 조회에 사용함)
    Optional<BillingKey> findByMemberNo(Long memberNo);

    // 회원의 빌링키 존재 여부
    boolean existsByMemberNo(Long memberNo);
}