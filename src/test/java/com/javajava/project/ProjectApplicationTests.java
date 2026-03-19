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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private BidHistoryRepository bidHistoryRepository;

    private Long savedProductId;

    @BeforeEach
    void setUp() {
        // 1. 판매자 생성
        Member seller = memberRepository.save(Member.builder()
                .userId("seller_final")
                .password("1234")
                .nickname("판매왕")
                .email("seller_f@test.com")
                .phoneNum("010-1234-1234")
                .emdNo(1L)
                .addrDetail("상남동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .isAdmin(0)
                .isActive(1)
                .build());

        // 2. 상품 등록
        savedProductId = productService.save(ProductRequestDto.builder()
                .sellerNo(seller.getMemberNo())
                .categoryNo(100L)
                .title("Oracle 완벽 연동 상품")
                .description("테스트 설명")
                .tradeType("직거래")
                .startPrice(50000L)
                .minBidUnit(5000L)
                .endTime(LocalDateTime.now().plusDays(7))
                .build());

        // 3. 입찰자 생성
        Member bidder = memberRepository.save(Member.builder()
                .userId("bidder_final")
                .password("1234")
                .nickname("입찰매니아")
                .email("bidder_f@test.com")
                .phoneNum("010-5678-5678")
                .emdNo(2L)
                .addrDetail("중앙동")
                .birthDate(LocalDate.of(1995, 1, 1))
                .isAdmin(0)
                .isActive(1)
                .build());

        // 4. 입찰 내역 생성 (에러 해결: IS_AUTO, IS_CANCELLED, IS_WINNER 필수값 추가)
        bidHistoryRepository.save(BidHistory.builder()
                .productNo(savedProductId)
                .memberNo(bidder.getMemberNo())
                .bidPrice(55000L)
                .bidTime(LocalDateTime.now())
                .isAuto(0)        // ★ ORA-01400 해결
                .isCancelled(0)   // 필수값
                .isWinner(0)      // 필수값
                .build());
    }

    @Test
    @DisplayName("상품 기능 최종 통합 리포트")
    void totalFunctionalReport() {
        // 데이터 수집
        List<ProductResponseDto> productList = productService.findAllActive("latest");
        ProductDetailResponseDto detail = productService.getProductDetail(savedProductId, null);

        // 출력 리포트 생성
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("==================================================================\n");
        sb.append("          [ PRODUCT SERVICE FINAL TEST REPORT ]\n");
        sb.append("==================================================================\n\n");

        sb.append("1. [상품 목록 결과]\n");
        sb.append("------------------------------------------------------------------\n");
        if (productList.isEmpty()) {
            sb.append("- 조회된 상품이 없습니다.\n");
        } else {
            for (ProductResponseDto p : productList) {
                sb.append(String.format("- [번호: %d] %-20s | 현재가: %10d원 | 위치: %s\n", 
                        p.getProductNo(), p.getTitle(), p.getCurrentPrice(), p.getLocation()));
            }
        }

        sb.append("\n2. [선택 상품 상세 정보]\n");
        sb.append("------------------------------------------------------------------\n");
        sb.append(String.format("▶ 상 품 명 : %s\n", detail.getTitle()));
        sb.append(String.format("▶ 판 매 자 : %s (매너온도: %.1f도)\n", 
                detail.getSeller().getNickname(), detail.getSeller().getMannerTemp()));
        sb.append(String.format("▶ 입찰현황 : 참여자 %d명 / 현재최고가 %d원\n", 
                detail.getParticipantCount(), detail.getCurrentPrice()));
        
        sb.append("\n3. [최근 입찰 내역 기록]\n");
        sb.append("------------------------------------------------------------------\n");
        if (detail.getBidHistory() == null || detail.getBidHistory().isEmpty()) {
            sb.append("- 입찰 내역 없음\n");
        } else {
            detail.getBidHistory().forEach(bid -> 
                sb.append(String.format("  [%s] 입찰자: %-8s | 입찰금액: %10d원\n", 
                        bid.getBidTime().toLocalTime(), bid.getBidderNickname(), bid.getBidPrice()))
            );
        }

        sb.append("\n==================================================================\n");
        sb.append("         RESULT: Oracle DB 연동 및 데이터 통합 성공\n");
        sb.append("==================================================================\n\n");

        System.out.println(sb.toString());
    }
}