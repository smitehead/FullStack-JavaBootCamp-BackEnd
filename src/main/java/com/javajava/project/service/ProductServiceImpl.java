package com.javajava.project.service;

import com.javajava.project.entity.Product;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long save(Product product) {
        // 초기 현재가는 시작가(START_PRICE)와 동일하게 설정
        product.setCurrentPrice(product.getStartPrice());
        return productRepository.save(product).getProductNo();
    }

    @Override
    public List<Product> findAllActive() {
        return productRepository.findByIsActiveAndIsDeleted(1, 0); // 활성 상태 상품 조회
    }

    @Override
    public Product findById(Long productNo) {
        return productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID: " + productNo));
    }

    @Override
    public List<Product> findByCategory(Long categoryNo) {
        return productRepository.findAll(); // 추후 필터 로직 확장
    }
}