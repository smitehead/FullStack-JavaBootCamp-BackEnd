package com.javajava.project.service;

import com.javajava.project.entity.Wishlist;
import com.javajava.project.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;

    @Override
    @Transactional
    public boolean toggleWishlist(Long memberNo, Long productNo) {
        Optional<Wishlist> existing = wishlistRepository.findByMemberNoAndProductNo(memberNo, productNo);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false; // 위시리스트에서 제거됨
        } else {
            Wishlist wishlist = Wishlist.builder()
                    .memberNo(memberNo)
                    .productNo(productNo)
                    .build();
            wishlistRepository.save(wishlist);
            return true; // 위시리스트에 추가됨
        }
    }
}
