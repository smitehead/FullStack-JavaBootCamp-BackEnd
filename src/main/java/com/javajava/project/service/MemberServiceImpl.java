package com.javajava.project.service;

import com.javajava.project.dto.MemberRequestDto;
import com.javajava.project.dto.ProductListResponseDto;
import com.javajava.project.dto.SellerProfileResponseDto;
import com.javajava.project.entity.Member;
import com.javajava.project.entity.Product;
import com.javajava.project.entity.ProductImage;
import com.javajava.project.repository.BidHistoryRepository;
import com.javajava.project.repository.MemberRepository;
import com.javajava.project.repository.ProductImageRepository;
import com.javajava.project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BidHistoryRepository bidHistoryRepository;

    @Override
    @Transactional
    public Long join(MemberRequestDto dto) {
        validateDuplicate(dto);

        if (dto.getBirthDate().plusYears(14).isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("만 14세 미만은 가입할 수 없습니다.");
        }

        Member member = Member.builder()
                .userId(dto.getUserId())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .phoneNum(dto.getPhoneNum())
                .emdNo(dto.getEmdNo())
                .addrDetail(dto.getAddrDetail())
                .birthDate(dto.getBirthDate())
                .marketingAgree(dto.getMarketingAgree() != null ? dto.getMarketingAgree() : 0)
                .build();

        memberRepository.save(member);
        return member.getMemberNo();
    }

    private void validateDuplicate(MemberRequestDto dto) {
        if (memberRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
        if (memberRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalStateException("이미 존재하는 닉네임입니다.");
        }
        if (memberRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }
    }

    @Override
    public Member findOne(Long memberNo) {
        return memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    @Override
    public boolean isUserIdDuplicate(String userId) {
        return memberRepository.existsByUserId(userId);
    }

    @Override
    public boolean isNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Override
    public boolean isEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updateProfileImage(Long memberNo, String url) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.setProfileImgUrl(url);
    }

    @Override
    public SellerProfileResponseDto getSellerProfile(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        // 삭제되지 않은 판매 상품만 조회
        List<Product> products = productRepository.findBySellerNo(memberNo).stream()
                .filter(p -> p.getIsDeleted() == 0)
                .collect(Collectors.toList());

        List<ProductListResponseDto> productDtos = List.of();
        if (!products.isEmpty()) {
            List<Long> productNos = products.stream()
                    .map(Product::getProductNo)
                    .collect(Collectors.toList());

            // 메인 이미지 배치 조회
            Map<Long, ProductImage> mainImageMap =
                    productImageRepository.findMainImagesByProductNos(productNos).stream()
                            .collect(Collectors.toMap(ProductImage::getProductNo, Function.identity(), (a, b) -> a));

            // 참여자 수 배치 조회
            Map<Long, Long> participantCountMap =
                    bidHistoryRepository.countDistinctParticipantsByProductNos(productNos).stream()
                            .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

            productDtos = products.stream().map(product -> {
                ProductImage mainImg = mainImageMap.get(product.getProductNo());
                List<String> imageUrls = mainImg != null
                        ? List.of("/api/images/" + mainImg.getUuidName())
                        : List.of();
                boolean isFinished = product.getEndTime().isBefore(LocalDateTime.now()) || product.getStatus() != 0;

                return ProductListResponseDto.builder()
                        .id(product.getProductNo())
                        .title(product.getTitle())
                        .location(product.getTradeAddrDetail())
                        .currentPrice(product.getCurrentPrice())
                        .endTime(product.getEndTime())
                        .participantCount(participantCountMap.getOrDefault(product.getProductNo(), 0L))
                        .status(isFinished ? "completed" : "active")
                        .images(imageUrls)
                        .isWishlisted(false)
                        .build();
            }).collect(Collectors.toList());
        }

        return SellerProfileResponseDto.builder()
                .sellerNo(member.getMemberNo())
                .nickname(member.getNickname())
                .profileImgUrl(member.getProfileImgUrl())
                .mannerTemp(member.getMannerTemp())
                .joinedAt(member.getJoinedAt())
                .products(productDtos)
                .build();
    }
}
