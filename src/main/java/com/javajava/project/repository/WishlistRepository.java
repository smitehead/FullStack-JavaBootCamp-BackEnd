package com.javajava.project.repository;

import com.javajava.project.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByMemberNoAndProductNo(Long memberNo, Long productNo);
    boolean existsByMemberNoAndProductNo(Long memberNo, Long productNo);
}
