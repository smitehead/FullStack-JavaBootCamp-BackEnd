package com.javajava.project.controller;

import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
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
    public ResponseEntity<List<ProductResponseDto>> getProducts(
            @RequestParam(name = "sort", defaultValue = "latest") String sort) {
        // 정렬 옵션을 받아 서비스 호출
        return ResponseEntity.ok(productService.findAllActive(sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable("id") Long productNo) {
        return ResponseEntity.ok(productService.findById(productNo));
    }
}