package com.javajava.project.domain.community.repository;

import com.javajava.project.domain.community.entity.ReviewTagDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewTagDefRepository extends JpaRepository<ReviewTagDef, Long> {

    List<ReviewTagDef> findByApplicableRoleIn(List<String> roles);

    List<ReviewTagDef> findByTagIdIn(List<Long> tagIds);

    /** REVIEW_TAG 중간 테이블을 통해 특정 리뷰의 태그명 목록을 반환 */
    @Query("SELECT d.tagName FROM ReviewTagDef d WHERE d.tagId IN " +
           "(SELECT rt.tagId FROM ReviewTag rt WHERE rt.reviewNo = :reviewNo)")
    List<String> findTagNamesByReviewNo(@Param("reviewNo") Long reviewNo);
}
