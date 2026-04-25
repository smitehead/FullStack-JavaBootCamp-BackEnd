package com.javajava.project.domain.community.repository;

import com.javajava.project.domain.community.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findByMemberNoOrderByCreatedAtDesc(Long memberNo, Pageable pageable);
    Page<Inquiry> findByMemberNoAndTypeOrderByCreatedAtDesc(Long memberNo, String type, Pageable pageable);

    Page<Inquiry> findByMemberNoAndTitleContainingOrderByCreatedAtDesc(Long memberNo, String keyword, Pageable pageable);
    Page<Inquiry> findByMemberNoAndTypeAndTitleContainingOrderByCreatedAtDesc(Long memberNo, String type, String keyword, Pageable pageable);

    Page<Inquiry> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}