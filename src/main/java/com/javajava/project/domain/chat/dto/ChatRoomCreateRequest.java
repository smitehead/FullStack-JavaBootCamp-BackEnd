package com.javajava.project.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequest {
    private Long buyerNo;      // 구매자(입찰자) 회원번호
    private Long sellerNo;     // 판매자 회원번호
    private Long productNo;    // 상품번호
}