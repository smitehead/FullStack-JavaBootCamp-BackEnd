package com.javajava.project.domain.product.controller;

import com.javajava.project.domain.product.dto.ProductQnaAnswerDto;
import com.javajava.project.domain.product.dto.ProductQnaRequestDto;
import com.javajava.project.domain.product.dto.ProductQnaResponseDto;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.repository.ProductRepository;
import com.javajava.project.domain.product.service.ProductQnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productNo}/qna")
@RequiredArgsConstructor
public class ProductQnaController {

    private final ProductQnaService productQnaService;
    private final ProductRepository productRepository;

    /** 문의 목록 조회 (비로그인 가능) */
    @GetMapping
    public ResponseEntity<List<ProductQnaResponseDto>> getList(@PathVariable Long productNo) {
        return ResponseEntity.ok(productQnaService.getQnaList(productNo));
    }

    /** 문의 등록 (로그인 필요) */
    @PostMapping
    public ResponseEntity<Long> create(
            @PathVariable Long productNo,
            @RequestBody ProductQnaRequestDto dto,
            Authentication authentication) {
        Long memberNo = getMemberNo(authentication);
        return ResponseEntity.ok(productQnaService.createQna(productNo, memberNo, dto));
    }

    /** 문의 삭제 (작성자만) */
    @DeleteMapping("/{qnaNo}")
    public ResponseEntity<Void> delete(
            @PathVariable Long productNo,
            @PathVariable Long qnaNo,
            Authentication authentication) {
        Long memberNo = getMemberNo(authentication);
        productQnaService.deleteQna(qnaNo, memberNo);
        return ResponseEntity.ok().build();
    }

    /** 판매자 답변 등록/수정 (판매자만) */
    @PostMapping("/{qnaNo}/answer")
    public ResponseEntity<Void> answer(
            @PathVariable Long productNo,
            @PathVariable Long qnaNo,
            @RequestBody ProductQnaAnswerDto dto,
            Authentication authentication) {
        Long sellerNo = getMemberNo(authentication);
        Long productSellerNo = getProductSellerNo(productNo);
        productQnaService.answerQna(qnaNo, sellerNo, productSellerNo, dto.getAnswer());
        return ResponseEntity.ok().build();
    }

    /** 판매자 답변 삭제 (판매자만) */
    @DeleteMapping("/{qnaNo}/answer")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable Long productNo,
            @PathVariable Long qnaNo,
            Authentication authentication) {
        Long sellerNo = getMemberNo(authentication);
        Long productSellerNo = getProductSellerNo(productNo);
        productQnaService.deleteAnswer(qnaNo, sellerNo, productSellerNo);
        return ResponseEntity.ok().build();
    }

    private Long getMemberNo(Authentication authentication) {
        if (authentication == null) throw new SecurityException("로그인이 필요합니다.");
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long)) throw new SecurityException("로그인이 필요합니다.");
        return (Long) principal;
    }

    private Long getProductSellerNo(Long productNo) {
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return product.getSellerNo();
    }
}
