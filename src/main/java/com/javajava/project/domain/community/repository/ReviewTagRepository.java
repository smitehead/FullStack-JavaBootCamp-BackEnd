package com.javajava.project.domain.community.repository;

import com.javajava.project.domain.community.entity.ReviewTag;
import com.javajava.project.domain.community.entity.ReviewTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewTagRepository extends JpaRepository<ReviewTag, ReviewTagId> {

    List<ReviewTag> findByReviewNo(Long reviewNo);

    void deleteByReviewNo(Long reviewNo);
}
