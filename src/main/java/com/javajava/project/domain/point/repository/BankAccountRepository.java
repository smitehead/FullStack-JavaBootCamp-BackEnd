// BankAccountRepository.java
package com.javajava.project.domain.point.repository;

import com.javajava.project.domain.point.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByMemberNoOrderByIsDefaultDescCreatedAtDesc(Long memberNo);
    void deleteByAccountNoAndMemberNo(Long accountNo, Long memberNo);
}