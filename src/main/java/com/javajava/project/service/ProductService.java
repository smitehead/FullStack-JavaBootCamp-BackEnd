package com.javajava.project.service;

import com.javajava.project.entity.Product;
import java.util.List;

public interface ProductService {
    Long save(Product product);
    List<Product> findAllActive();
    Product findById(Long productNo);
    List<Product> findByCategory(Long categoryNo);
}