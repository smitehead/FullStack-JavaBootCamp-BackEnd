package com.javajava.project;

import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.entity.BidHistory;
import com.javajava.project.entity.Member;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@Transactional // 테스트 완료 후 DB 데이터를 롤백하여 깨끗하게 유지합니다.
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BidHistoryRepository bidHistoryRepository;

    private Member seller;
    private Long savedProductId;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 판매자 생성
        seller = memberRepository.save(Member.builder()
                .userId("test_seller_01")
                .nickname("장사천재")
                .mannerTemp(42.5)
                .build());

        // 2. 테스트용 상품 등록
        ProductRequestDto requestDto = ProductRequestDto.builder()
                .sellerNo(seller.getMemberNo())
                .title("테스트용 경매 상품")
                .description("상세 페이지 테스트를 위한 상품 설명입니다.")
                .tradeType("직거래")
                .startPrice(100000L)
                .minBidUnit(10000L)
                .endTime(LocalDateTime.now().plusDays(3))
                .build();
        
        savedProductId = productService.save(requestDto);

        // 3. 테스트용 입찰 내역 추가
        Member bidder = memberRepository.save(Member.builder().nickname("입찰열정남").build());
        bidHistoryRepository.save(BidHistory.builder()
                .productNo(savedProductId)
                .memberNo(bidder.getMemberNo())
                .bidPrice(120000L)
                .bidTime(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("상품 목록 조회 및 정렬 결과 콘솔 출력")
    void printProductList() {
        System.out.println("\n==================== [1. 상품 목록 조회 결과] ====================");
        
        // 'latest' 정렬 옵션으로 조회
        List<ProductResponseDto> productList = productService.findAllActive("latest");

        productList.forEach(p -> {
            System.out.println("상품번호: " + p.getProductNo());
            System.out.println("상품명: " + p.getTitle());
            System.out.println("현재가: " + p.getCurrentPrice() + "원");
            System.out.println("위치: " + p.getLocation());
            System.out.println("--------------------------------------------------");
        });
        System.out.println("===============================================================\n");
    }

    @Test
    @DisplayName("상품 상세 페이지 통합 정보 콘솔 출력")
    void printProductDetail() {
        System.out.println("\n==================== [2. 상품 상세 페이지 조회 결과] ====================");
        
        // 상세 정보 조회 실행
        ProductDetailResponseDto detail = productService.getProductDetail(savedProductId, null);

        System.out.println("[상품 정보]");
        System.out.println("- 제목: " + detail.getTitle());
        System.out.println("- 설명: " + detail.getDescription());
        System.out.println("- 시작가: " + detail.getStartPrice() + "원");
        System.out.println("- 현재가: " + detail.getCurrentPrice() + "원");
        System.out.println("- 종료시간: " + detail.getEndTime());

        System.out.println("\n[판매자 정보]");
        System.out.println("- 닉네임: " + detail.getSeller().getNickname());
        System.out.println("- 매너온도: " + detail.getSeller().getMannerTemp() + "도");

        System.out.println("\n[입찰 내역]");
        if (detail.getBidHistory().isEmpty()) {
            System.out.println("- 입찰 내역이 없습니다.");
        } else {
            detail.getBidHistory().forEach(bid -> 
                System.out.println("- [" + bid.getBidTime() + "] " + bid.getBidderNickname() + " : " + bid.getBidPrice() + "원")
            );
        }
        System.out.println("==================================================================\n");
    }
}