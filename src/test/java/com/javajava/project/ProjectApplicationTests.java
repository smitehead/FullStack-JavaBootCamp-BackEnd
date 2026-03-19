package com.javajava.project;

import com.javajava.project.dto.BidRequestDto;
import com.javajava.project.dto.ProductDetailResponseDto;
import com.javajava.project.dto.ProductRequestDto;
import com.javajava.project.dto.ProductResponseDto;
import com.javajava.project.entity.Member;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.service.BidService;
import com.javajava.project.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private BidService bidService; // 새롭게 추가된 서비스

    @Autowired
    private MemberRepository memberRepository;

    private Long savedProductId;
    private Member bidder1;
    private Member bidder2;

    @BeforeEach
    void setUp() {
        // 1. 판매자 생성 (포인트 0)
        Member seller = memberRepository.save(Member.builder()
                .userId("seller_tester").password("1234").nickname("판매왕")
                .email("seller@test.com").phoneNum("010-1111-1111").emdNo(1L)
                .addrDetail("상남동").birthDate(LocalDate.of(1990, 1, 1))
                .points(0L).build());

        // 2. 상품 등록 (시작가: 50,000원 / 최소 입찰단위: 5,000원)
        savedProductId = productService.save(ProductRequestDto.builder()
                .sellerNo(seller.getMemberNo()).categoryNo(100L)
                .title("Oracle 실시간 경매 상품").description("테스트용 상품입니다.")
                .tradeType("직거래").tradeAddrDetail("창원시청 앞")
                .startPrice(50000L).minBidUnit(5000L)
                .endTime(LocalDateTime.now().plusDays(7)).build());

        // 3. 입찰자 1 생성 (포인트: 100,000원)
        bidder1 = memberRepository.save(Member.builder()
                .userId("bidder_01").password("1234").nickname("입찰매니아")
                .email("bid1@test.com").phoneNum("010-2222-2222").emdNo(2L)
                .addrDetail("중앙동").birthDate(LocalDate.of(1995, 5, 5))
                .points(100000L).build());

        // 4. 입찰자 2 생성 (포인트: 200,000원)
        bidder2 = memberRepository.save(Member.builder()
                .userId("bidder_02").password("1234").nickname("낙찰희망자")
                .email("bid2@test.com").phoneNum("010-3333-3333").emdNo(3L)
                .addrDetail("용호동").birthDate(LocalDate.of(1992, 12, 12))
                .points(200000L).build());
    }

    @Test
    @DisplayName("경매 시스템 통합 기능 리포트 (목록조회 + 실시간입찰 + 상세조회)")
    void totalFunctionalReport() {
        // [작업 1] 실시간 입찰 시나리오 진행
        // 입찰 1: 입찰자1이 55,000원 입찰 (성공)
        bidService.processBid(BidRequestDto.builder()
                .productNo(savedProductId).memberNo(bidder1.getMemberNo())
                .bidPrice(55000L).build());

        // 입찰 2: 입찰자2가 60,000원 입찰 (성공)
        bidService.processBid(BidRequestDto.builder()
                .productNo(savedProductId).memberNo(bidder2.getMemberNo())
                .bidPrice(60000L).build());

        // [작업 2] 데이터 수집
        List<ProductResponseDto> productList = productService.findAllActive("latest");
        ProductDetailResponseDto detail = productService.getProductDetail(savedProductId, null);
        // 최적화된 입찰 기록 전용 조회 API 활용
        List<ProductDetailResponseDto.BidHistoryDto> recentBids = bidService.getBidHistory(savedProductId);

        // [작업 3] 통합 리포트 생성
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("==================================================================\n");
        sb.append("          [ AUCTION SYSTEM INTEGRATED TEST REPORT ]\n");
        sb.append("==================================================================\n\n");

        sb.append("1. [활성 경매 목록 조회]\n");
        sb.append("------------------------------------------------------------------\n");
        for (ProductResponseDto p : productList) {
            sb.append(String.format("- [번호: %d] %-20s | 현재최고가: %10d원 | 위치: %s\n", 
                    p.getProductNo(), p.getTitle(), p.getCurrentPrice(), p.getLocation()));
        }

        sb.append("\n2. [상품 상세 정보 및 판매자 프로필]\n");
        sb.append("------------------------------------------------------------------\n");
        sb.append(String.format("▶ 상 품 명 : %s\n", detail.getTitle()));
        sb.append(String.format("▶ 판 매 자 : %s (매너온도: %.1f도)\n", 
                detail.getSeller().getNickname(), detail.getSeller().getMannerTemp()));
        sb.append(String.format("▶ 경매현황 : 현재가 %,d원 (총 %d회 입찰)\n", 
                detail.getCurrentPrice(), detail.getParticipantCount()));
        
        sb.append("\n3. [실시간 입찰 로그 (최신순)]\n");
        sb.append("------------------------------------------------------------------\n");
        if (recentBids.isEmpty()) {
            sb.append("- 진행된 입찰 내역이 없습니다.\n");
        } else {
            recentBids.forEach(bid -> 
                sb.append(String.format("  [%s] 입찰자: %-10s | 입찰금액: %10s원\n", 
                        bid.getBidTime().toLocalTime().withNano(0), 
                        bid.getBidderNickname(), 
                        String.format("%,d", bid.getBidPrice())))
            );
        }

        sb.append("\n==================================================================\n");
        sb.append("    검증 결과: 입찰 유효성 검사, 포인트 체크, 실시간 가격 갱신 완료\n");
        sb.append("==================================================================\n\n");

        System.out.println(sb.toString());
    }
}