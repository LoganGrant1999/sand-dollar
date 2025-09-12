package com.sanddollar.repository;

import com.sanddollar.entity.Account;
import com.sanddollar.entity.BalanceSnapshot;
import com.sanddollar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceSnapshotRepository extends JpaRepository<BalanceSnapshot, Long> {
    Optional<BalanceSnapshot> findTopByAccountOrderByAsOfDesc(Account account);
    
    @Query("SELECT SUM(bs.availableCents) FROM BalanceSnapshot bs " +
           "WHERE bs.account.user.id = :userId " +
           "AND bs.id IN (SELECT MAX(bs2.id) FROM BalanceSnapshot bs2 GROUP BY bs2.account.id)")
    Long getTotalAvailableBalanceForUser(@Param("userId") Long userId);
    
    @Query("SELECT bs FROM BalanceSnapshot bs WHERE bs.account.user = :user " +
           "AND bs.id IN (SELECT MAX(bs2.id) FROM BalanceSnapshot bs2 WHERE bs2.account.user = :user GROUP BY bs2.account.id)")
    List<BalanceSnapshot> findRecentByUser(@Param("user") User user);
    
    @Query("SELECT bs FROM BalanceSnapshot bs WHERE bs.account.user = :user " +
           "AND bs.asOf >= :since ORDER BY bs.asOf ASC")
    List<BalanceSnapshot> findByUserSince(@Param("user") User user, @Param("since") java.time.Instant since);
    
    // Additional methods for mock data management
    List<BalanceSnapshot> findByAccountOrderByAsOfDesc(Account account);
    void deleteByAccount(Account account);
}