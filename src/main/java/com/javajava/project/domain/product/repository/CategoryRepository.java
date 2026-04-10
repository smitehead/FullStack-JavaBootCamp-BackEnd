package com.javajava.project.domain.product.repository;

import com.javajava.project.domain.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // 상위 카테고리 번호로 하위 카테고리 목록 조회
    List<Category> findByParentNo(Long parentNo);
    
    // 특정 레벨(Depth)의 카테고리 목록 조회
    List<Category> findByDepth(Integer depth);
}
