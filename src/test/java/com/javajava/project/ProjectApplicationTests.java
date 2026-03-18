package com.javajava.project;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.entity.Product;
import com.javajava.project.service.MemberService;
import com.javajava.project.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest
class ProjectApplicationTests {

    @Autowired
    private ProductService productService;

    @Autowired
    private MemberService memberService;

    @Test
    @Transactional // 테스트 완료 후 DB 데이터를 롤백하고 싶지 않다면 이 줄을 주석 처리하세요.
    @DisplayName("상품 등록 및 DB 저장 확인 테스트")
    void productSaveTest() {
        // 1. 테스트용 판매자(회원) 등록
        MemberRequestDto memberDto = MemberRequestDto.builder()
                .userId("seller01")
                .password("1234")
                .nickname("경매장인")
                .email("seller@test.com")
                .phoneNum("010-1234-5678")
                .emdNo(11110L)
                .addrDetail("서울시 강남구")
                .birthDate(LocalDate.of(1995, 5, 20))
                .build();
        
        Long memberNo = memberService.join(memberDto);
        System.out.println(">>> [테스트] 판매자 등록 완료. 번호: " + memberNo);

        // 2. 테스트용 상품 데이터 생성 (ProductRequestDto 활용)
        ProductRequestDto productDto = ProductRequestDto.builder()
                .sellerNo(memberNo)
                .categoryNo(1L) // 실제 DB에 CATEGORY 테이블 데이터가 없어도 Long 값으로 들어감
                .title("테스트용 경매 상품")
                .description("이것은 코드 연동 확인을 위한 테스트 상품입니다.")
                .tradeType("택배")
                .tradeEmdNo(11110L)
                .tradeAddrDetail("강남역 5번 출구")
                .startPrice(50000L)
                .buyoutPrice(100000L)
                .minBidUnit(5000L)
                .endTime(LocalDateTime.now().plusDays(7)) // 7일 후 종료
                .build();

        // 3. 서비스 호출하여 상품 저장
        Long productNo = productService.save(productDto);
        System.out.println(">>> [테스트] 상품 등록 완료. 번호: " + productNo);

        // 4. DB에서 다시 조회하여 콘솔에 결과 출력
        Product result = productService.findById(productNo);
        
        System.out.println("\n===== [상품 등록 결과 확인] =====");
        System.out.println("등록 번호: " + result.getProductNo());
        System.out.println("상품 제목: " + result.getTitle());
        System.out.println("판매자 번호: " + result.getSellerNo());
        System.out.println("설정된 현재가: " + result.getCurrentPrice() + "원 (시작가와 동일 여부 확인)");
        System.out.println("경매 종료 시간: " + result.getEndTime());
        System.out.println("활성 상태: " + (result.getIsActive() == 1 ? "진행 중" : "종료"));
        System.out.println("===============================\n");
    }
}