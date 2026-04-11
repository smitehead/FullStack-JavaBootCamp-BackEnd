package com.javajava.project.domain.community.repository;

import com.javajava.project.domain.community.entity.InquiryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryImageRepository extends JpaRepository<InquiryImage, Long> {
    List<InquiryImage> findByInquiryNo(Long inquiryNo);
}
