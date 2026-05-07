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

    /** 여러 리뷰의 태그명을 한 번에 조회 — N+1 방지용 배치 쿼리 */
    @Query("SELECT rt.reviewNo, d.tagName FROM ReviewTagDef d JOIN ReviewTag rt ON d.tagId = rt.tagId WHERE rt.reviewNo IN :reviewNos")
    List<Object[]> findTagNamesByReviewNos(@Param("reviewNos") List<Long> reviewNos);
}
