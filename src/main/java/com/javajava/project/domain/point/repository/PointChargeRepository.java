package com.javajava.project.domain.point.repository;

import com.javajava.project.domain.point.entity.PointCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointChargeRepository extends JpaRepository<PointCharge, Long> {
    // merchantUid로 결제내역 조회 (멱등성 처리 시 중복 확인)
    Optional<PointCharge> findByMerchantUid(String merchantUid);

    // 회원의 충전 내역 페이징을 최신순으로 조회
    Page<PointCharge> findByMemberNoOrderByChargedAtDesc(Long memberNo, Pageable pageable);

    // PENDING 상태이고 특정 시각 이전인 레코드 조회
    List<PointCharge> findByStatusAndChargedAtBefore(String status, LocalDateTime before);
}
