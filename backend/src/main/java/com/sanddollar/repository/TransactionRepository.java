package com.sanddollar.repository;

import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountUserOrderByDateDesc(User user);
    Transaction findByExternalId(String externalId);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<Transaction> findByUserAndDateRange(@Param("user") User user, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user AND t.date >= :startDate AND t.date <= :endDate AND t.isTransfer = false ORDER BY t.date DESC")
    List<Transaction> findSpendingByUserAndDateRange(@Param("user") User user, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t.categoryTop, SUM(ABS(t.amountCents)) as total FROM Transaction t " +
           "WHERE t.account.user = :user AND t.date >= :startDate AND t.date <= :endDate " +
           "AND t.amountCents < 0 AND t.isTransfer = false " +
           "GROUP BY t.categoryTop ORDER BY total DESC")
    List<Object[]> getSpendingByCategory(@Param("user") User user, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);
    
    @Query("SELECT DATE(t.date) as day, SUM(ABS(t.amountCents)) as total FROM Transaction t " +
           "WHERE t.account.user = :user AND t.date >= :startDate AND t.date <= :endDate " +
           "AND t.amountCents < 0 AND t.isTransfer = false " +
           "GROUP BY DATE(t.date) ORDER BY day DESC")
    List<Object[]> getDailySpending(@Param("user") User user, 
                                  @Param("startDate") LocalDate startDate, 
                                  @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user AND t.categoryTop = :category " +
           "AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<Transaction> findByCategoryAndDateRange(@Param("user") User user, 
                                               @Param("category") String category,
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    
    // Additional methods for mock data management
    List<Transaction> findByAccount(com.sanddollar.entity.Account account);
    void deleteByAccount(com.sanddollar.entity.Account account);
}