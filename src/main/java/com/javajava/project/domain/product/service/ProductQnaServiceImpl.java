package com.javajava.project.domain.product.service;

import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.notification.service.NotificationService;
import com.javajava.project.domain.product.dto.ProductQnaRequestDto;
import com.javajava.project.domain.product.dto.ProductQnaResponseDto;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.entity.ProductQna;
import com.javajava.project.domain.product.repository.ProductQnaRepository;
import com.javajava.project.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQnaServiceImpl implements ProductQnaService {

    private final ProductQnaRepository productQnaRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductQnaResponseDto> getQnaList(Long productNo) {
        List<ProductQna> qnas = productQnaRepository.findByProductNoOrderByCreatedAtAsc(productNo);

        List<Long> memberNos = qnas.stream().map(ProductQna::getMemberNo).distinct().toList();
        Map<Long, String> nicknameMap = memberRepository.findAllById(memberNos).stream()
                .collect(Collectors.toMap(Member::getMemberNo, Member::getNickname));

        return qnas.stream()
                .map(q -> ProductQnaResponseDto.from(q, nicknameMap.getOrDefault(q.getMemberNo(), "알 수 없음")))
                .toList();
    }

    @Override
    @Transactional
    public Long createQna(Long productNo, Long memberNo, ProductQnaRequestDto dto) {
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("문의 내용을 입력해주세요.");
        }

        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        ProductQna qna = ProductQna.builder()
                .productNo(productNo)
                .memberNo(memberNo)
                .content(dto.getContent().trim())
                .build();
        productQnaRepository.save(qna);

        // 판매자에게 알림 전송
        String writerNickname = memberRepository.findById(memberNo)
                .map(Member::getNickname).orElse("누군가");
        notificationService.sendAndSaveNotification(
                product.getSellerNo(),
                "QNA",
                "[" + product.getTitle() + "] " + writerNickname + "님이 상품문의를 남겼습니다.",
                "/products/" + productNo
        );

        return qna.getPrdtQnaNo();
    }

    @Override
    @Transactional
    public void deleteQna(Long qnaNo, Long memberNo) {
        ProductQna qna = productQnaRepository.findById(qnaNo)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));
        if (!qna.getMemberNo().equals(memberNo)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        productQnaRepository.delete(qna);
    }

    @Override
    @Transactional
    public void answerQna(Long qnaNo, Long sellerNo, Long productSellerNo, String answer) {
        if (!sellerNo.equals(productSellerNo)) {
            throw new SecurityException("답변 권한이 없습니다.");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("답변 내용을 입력해주세요.");
        }

        ProductQna qna = productQnaRepository.findById(qnaNo)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));
        qna.setAnswer(answer.trim());
        qna.setAnsweredAt(LocalDateTime.now());

        // 문의 작성자에게 알림 전송
        Product product = productRepository.findById(qna.getProductNo())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        notificationService.sendAndSaveNotification(
                qna.getMemberNo(),
                "QNA_ANSWER",
                "[" + product.getTitle() + "] 판매자가 상품문의에 답변을 남겼습니다.",
                "/products/" + qna.getProductNo()
        );
    }

    @Override
    @Transactional
    public void deleteAnswer(Long qnaNo, Long sellerNo, Long productSellerNo) {
        if (!sellerNo.equals(productSellerNo)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        ProductQna qna = productQnaRepository.findById(qnaNo)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));
        qna.setAnswer(null);
        qna.setAnsweredAt(null);
    }
}
