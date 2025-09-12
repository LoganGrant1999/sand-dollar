package com.sanddollar.repository;

import com.sanddollar.entity.Account;
import com.sanddollar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUser(User user);
    Optional<Account> findByAccountId(String accountId);
    Account findByAccountIdAndUser(String accountId, User user);
    
    @Query("SELECT a FROM Account a WHERE a.user = :user AND a.type IN ('depository', 'investment')")
    List<Account> findCashAccountsByUser(@Param("user") User user);
    
    // Additional methods for mock data management
    void deleteByUser(User user);
}