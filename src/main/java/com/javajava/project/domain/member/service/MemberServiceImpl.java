package com.javajava.project.domain.member.service;

import com.javajava.project.domain.member.dto.*;
import com.javajava.project.domain.product.dto.ProductListResponseDto;
import com.javajava.project.domain.member.entity.Member;
import com.javajava.project.domain.product.entity.Product;
import com.javajava.project.domain.product.entity.ProductImage;
import com.javajava.project.domain.bid.repository.BidHistoryRepository;
import com.javajava.project.domain.member.repository.MemberRepository;
import com.javajava.project.domain.member.repository.BlockedUserRepository;
import com.javajava.project.domain.product.repository.ProductImageRepository;
import com.javajava.project.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final BlockedUserRepository blockedUserRepository;
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
                .addrRoad(dto.getAddrRoad())
                .addrDetail(dto.getAddrDetail())
                .addrShort(dto.getAddrShort())
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
    public MemberResponseDto findOne(Long memberNo) {
        return memberRepository.findById(memberNo)
                .map(MemberResponseDto::from)
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

        // 삭제되지 않은 판매 상품만 조회 (최신순)
        List<Product> products = productRepository.findBySellerNoOrderByProductNoDesc(memberNo).stream()
                .filter(p -> p.getIsDeleted() == 0)
                .toList();

        List<ProductListResponseDto> productDtos = List.of();
        if (!products.isEmpty()) {
            List<Long> productNos = products.stream()
                    .map(Product::getProductNo)
                    .toList();

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
            }).toList();
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

    @Override
    @Transactional
    public void updateProfile(Long memberNo, MemberProfileUpdateDto dto) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 닉네임 변경 시 중복 확인
        if (!member.getNickname().equals(dto.getNickname())
                && memberRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        member.setNickname(dto.getNickname());
        member.setPhoneNum(dto.getPhoneNum());
        member.setAddrRoad(dto.getAddrRoad());
        member.setAddrDetail(dto.getAddrDetail());
        member.setAddrShort(dto.getAddrShort());
    }

    @Override
    @Transactional
    public void changePassword(Long memberNo, PasswordChangeDto dto) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        member.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    }

    @Override
    @Transactional
    public void updateNotificationSetting(Long memberNo, NotificationSettingDto dto) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (dto.getAuctionEnd() != null) member.setNotifyAuctionEnd(dto.getAuctionEnd() ? 1 : 0);
        if (dto.getNewBid() != null)     member.setNotifyNewBid(dto.getNewBid() ? 1 : 0);
        if (dto.getChat() != null)       member.setNotifyChat(dto.getChat() ? 1 : 0);
        if (dto.getMarketing() != null) {
            member.setNotifyMarketing(dto.getMarketing() ? 1 : 0);
            member.setMarketingAgree(dto.getMarketing() ? 1 : 0);
        }
    }

    @Override
    @Transactional
    public void withdraw(Long memberNo, WithdrawMemberDto dto) {
        Member member = memberRepository.findByIdWithLock(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 본인 비밀번호 확인
        if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 포인트 잔액 확인
        if (member.getPoints() > 0) {
            throw new IllegalStateException("포인트를 먼저 출금해 주세요. 현재 잔액: " + member.getPoints() + "P");
        }

        // 진행 중인 경매 확인 (status=0: active, isDeleted=0: not deleted)
        long activeCount = productRepository.countBySellerNoAndStatusAndIsDeleted(memberNo, 0, 0);
        if (activeCount > 0) {
            throw new IllegalStateException("진행 중인 경매가 있어 탈퇴할 수 없습니다.");
        }

        member.setIsActive(0);
        member.setWithdrawReason(dto.getReason());
        member.setWithdrawnAt(LocalDateTime.now());
        member.setCurrentToken(null); // 토큰 무효화

        log.info("[Member] 회원 탈퇴. memberNo={}", memberNo);
    }

    @Override
    @Transactional
    public void updateEmail(Long memberNo, String email) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }
        member.setEmail(email);
    }

    @Override
    public MemberProfileResponseDto getProfile(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        return MemberProfileResponseDto.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .phoneNum(member.getPhoneNum())
                .addrRoad(member.getAddrRoad())
                .addrDetail(member.getAddrDetail())
                .addrShort(member.getAddrShort())
                .notifyAuctionEnd(member.getNotifyAuctionEnd())
                .notifyNewBid(member.getNotifyNewBid())
                .notifyChat(member.getNotifyChat())
                .marketingAgree(member.getMarketingAgree())
                .build();
    }

    @Override
    public List<BlockedUserResponseDto> getBlockedUsers(Long memberNo) {
        return blockedUserRepository.findByIdMemberNo(memberNo).stream()
                .map(b -> {
                    Member target = memberRepository.findById(b.getId().getBlockedMemberNo()).orElse(null);
                    if (target == null) return null;
                    return BlockedUserResponseDto.builder()
                            .id(target.getMemberNo())
                            .nickname(target.getNickname())
                            .profileImage(target.getProfileImgUrl())
                            .mannerTemp(target.getMannerTemp())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public void blockUser(Long memberNo, Long targetMemberNo) {
        // ... 생략 (Controller에서 직접 구현되어 있을 수 있음)
    }

    @Override
    @Transactional
    public void unblockUser(Long memberNo, Long targetMemberNo) {
        blockedUserRepository.deleteByIdMemberNoAndIdBlockedMemberNo(memberNo, targetMemberNo);
    }
}
