package com.javajava.project.controller;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.entity.Product;
import com.javajava.project.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Long> registerProduct(@RequestBody ProductRequestDto productDto) {
        return ResponseEntity.ok(productService.save(productDto));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(productService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductDetail(@PathVariable("id") Long productNo) {
        return ResponseEntity.ok(productService.findById(productNo));
    }
}