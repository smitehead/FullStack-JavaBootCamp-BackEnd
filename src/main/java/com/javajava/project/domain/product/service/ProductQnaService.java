package com.javajava.project.domain.product.service;

import com.javajava.project.domain.product.dto.ProductQnaRequestDto;
import com.javajava.project.domain.product.dto.ProductQnaResponseDto;

import java.util.List;

public interface ProductQnaService {

    List<ProductQnaResponseDto> getQnaList(Long productNo);

    Long createQna(Long productNo, Long memberNo, ProductQnaRequestDto dto);

    void deleteQna(Long qnaNo, Long memberNo);

    void answerQna(Long qnaNo, Long sellerNo, Long productSellerNo, String answer);

    void deleteAnswer(Long qnaNo, Long sellerNo, Long productSellerNo);
}
