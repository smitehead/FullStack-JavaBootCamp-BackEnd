package com.javajava.project;

import com.javajava.project.domain.bid.dto.BidRequestDto;
import com.javajava.project.domain.product.dto.ProductDetailResponseDto;
import com.javajava.project.domain.product.dto.ProductListResponseDto;
import com.javajava.project.domain.product.dto.ProductRequestDto;
import com.javajava.project.domain.product.dto.ProductResponseDto;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.bid.service.BidService;
import com.javajava.project.domain.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProjectApplicationTests {

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
                .email("seller@test.com").phoneNum("010-1111-1111").addrRoad("경남 창원시 성산구 상남로 123")
                .addrDetail("상남동")
                .addrShort("창원 성산구 상남동").birthDate(LocalDate.of(1990, 1, 1))
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
                .email("bid1@test.com").phoneNum("010-2222-2222").addrRoad("경남 창원시 성산구 상남로 123")
        .addrDetail("상남동")
        .addrShort("창원 성산구 상남동").birthDate(LocalDate.of(1995, 5, 5))
                .points(100000L).build());

        // 4. 입찰자 2 생성 (포인트: 200,000원)
        bidder2 = memberRepository.save(Member.builder()
                .userId("bidder_02").password("1234").nickname("낙찰희망자")
                .email("bid2@test.com").phoneNum("010-3333-3333").addrRoad("경남 창원시 성산구 상남로 123")
        .addrDetail("상남동")
        .addrShort("창원 성산구 상남동").birthDate(LocalDate.of(1992, 12, 12))
                .points(200000L).build());
    }

    @Test
    @DisplayName("경매 시스템 통합 시나리오 리포트 (페이징 + 입찰 + 환불)")
    void totalFunctionalReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("==================================================================\n");
        sb.append("          [ 🚀 AUCTION SYSTEM INTEGRATED TEST REPORT ]\n");
        sb.append("==================================================================\n\n");

        // [1] 페이징 기반 상품 목록 조회
        sb.append("1. [활성 경매 목록 조회 (페이징 적용)]\n");
        sb.append("------------------------------------------------------------------\n");
        Page<ProductListResponseDto> page = productService.getProductList(1, 16, null, null, null, null, null, null, null, null, "latest", null);
        assertNotNull(page);
        page.getContent().forEach(p -> 
            sb.append(String.format("- [번호: %d] %-20s | 현재최고가: %10d원 | 위치: %s\n", 
                    p.getId(), p.getTitle(), p.getCurrentPrice(), p.getLocation()))
        );

        // [2] 실시간 입찰 시나리오 진행
        sb.append("\n2. [실시간 입찰 및 포인트 변동 처리]\n");
        sb.append("------------------------------------------------------------------\n");
        
        // 입찰 1
        bidService.processBid(BidRequestDto.builder().productNo(savedProductId).memberNo(bidder1.getMemberNo()).bidPrice(55000L).build());
        sb.append(String.format("- 입찰자1(포인트 10만)이 55,000원 입찰 -> 성공! (잔액: %,d원)\n", memberRepository.findById(bidder1.getMemberNo()).get().getPoints()));

        // 입찰 2 (상위 입찰)
        bidService.processBid(BidRequestDto.builder().productNo(savedProductId).memberNo(bidder2.getMemberNo()).bidPrice(60000L).build());

        Member refundedBidder1 = memberRepository.findById(bidder1.getMemberNo()).orElseThrow();
        Member updatedBidder2 = memberRepository.findById(bidder2.getMemberNo()).orElseThrow();
        assertEquals(100000L, refundedBidder1.getPoints()); // 환불 완료 검증
        
        sb.append(String.format("- 입찰자2(포인트 20만)가 60,000원 상위 입찰 -> 성공! (잔액: %,d원)\n", updatedBidder2.getPoints()));
        sb.append(String.format("  * 기존 최고가 입찰자1 포인트 100%% 자동 환불 복구 확인! (현재: %,d원)\n", refundedBidder1.getPoints()));

        // [3] 상품 상세 정보 및 입찰 기록 조회
        ProductDetailResponseDto detail = productService.getProductDetail(savedProductId, null);
        List<ProductDetailResponseDto.BidHistoryDto> recentBids = bidService.getBidHistory(savedProductId);

        sb.append("\n3. [최종 상품 상세 정보 및 참여 현황]\n");
        sb.append("------------------------------------------------------------------\n");
        sb.append(String.format("▶ 상 품 명 : %s\n", detail.getTitle()));
        sb.append(String.format("▶ 판 매 자 : %s (매너온도: %.1f도)\n", detail.getSeller().getNickname(), detail.getSeller().getMannerTemp()));
        sb.append(String.format("▶ 경매현황 : 현재가 %,d원 (총 %d명 참여)\n", detail.getCurrentPrice(), detail.getParticipantCount()));
        
        recentBids.forEach(bid -> 
            sb.append(String.format("  [%s] 입찰자: %-10s | 입찰금액: %,d원\n", 
                    bid.getBidTime().toLocalTime().withNano(0), bid.getBidderNickname(), bid.getBidPrice()))
        );

        sb.append("\n==================================================================\n");
        sb.append("    ✨ 검증 결과: 페이징, 입찰, 자동 환불 시스템 정상 작동 확인! ✨\n");
        sb.append("==================================================================\n\n");

        System.out.println(sb.toString());
    }
}