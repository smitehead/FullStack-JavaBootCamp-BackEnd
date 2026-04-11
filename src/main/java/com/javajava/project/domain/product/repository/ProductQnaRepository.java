package com.javajava.project.domain.product.repository;

import com.javajava.project.domain.product.entity.ProductQna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductQnaRepository extends JpaRepository<ProductQna, Long> {

    List<ProductQna> findByProductNoOrderByCreatedAtAsc(Long productNo);

    long countByProductNo(Long productNo);
}
