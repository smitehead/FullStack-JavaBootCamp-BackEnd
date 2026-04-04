// PointWithdrawRepository.java
package com.javajava.project.domain.point.repository;

import com.javajava.project.domain.point.entity.PointWithdraw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PointWithdrawRepository extends JpaRepository<PointWithdraw, Long> {
    Page<PointWithdraw> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<PointWithdraw> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<PointWithdraw> findByMemberNoOrderByCreatedAtDesc(Long memberNo);
}