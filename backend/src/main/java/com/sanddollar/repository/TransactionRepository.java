package com.sanddollar.repository;

import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountUserOrderByDateDesc(User user);
    Transaction findByExternalId(String externalId);
    Optional<Transaction> findByPlaidTransactionId(String plaidTransactionId);
    Optional<Transaction> findByPendingTransactionId(String pendingTransactionId);

    @Query("SELECT t FROM Transaction t WHERE t.account.user = :user AND t.pending = false " +
           "AND t.date >= :startDate AND t.date <= :endDate")
    List<Transaction> findPostedByUserAndDateRange(@Param("user") User user,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
    
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

    // Methods for local profile testing
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.user.id = :userId AND FUNCTION('TO_CHAR', t.date, 'YYYY-MM') = :month")
    long countByAccountUserIdAndMonth(@Param("userId") Long userId, @Param("month") String month);

    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId ORDER BY t.date DESC")
    List<Transaction> findByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(t.amountCents) FROM Transaction t WHERE t.account.user.id = :userId AND t.date >= :startDate AND t.date <= :endDate AND t.amountCents > 0")
    Long sumIncomeByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Methods for goal feasibility analysis
    @Query("SELECT SUM(t.amountCents) FROM Transaction t WHERE t.account.user.id = :userId AND t.date >= :startDate AND t.date <= :endDate AND t.amountCents > 0 AND t.isTransfer = false")
    Long getTotalIncomeForUserInPeriod(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.amountCents) FROM Transaction t WHERE t.account.user.id = :userId AND t.date >= :startDate AND t.date <= :endDate AND t.amountCents < 0 AND t.isTransfer = false")
    Long getTotalExpensesForUserInPeriod(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t.categoryTop, SUM(ABS(t.amountCents)) FROM Transaction t WHERE t.account.user.id = :userId AND t.date >= :startDate AND t.date <= :endDate AND t.amountCents < 0 AND t.isTransfer = false GROUP BY t.categoryTop")
    List<Object[]> getCategorySpendingForUserInPeriod(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Method for auto-detecting goal contributions
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId AND t.date >= :cutoffDate AND t.amountCents > 0 AND " +
           "(LOWER(t.name) LIKE '%sand dollar%' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :goalName, '%')) OR " +
           "LOWER(t.merchantName) LIKE '%sand dollar%' OR LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :goalName, '%')) OR " +
           "t.isTransfer = true) ORDER BY t.date DESC")
    List<Transaction> findPotentialGoalContributions(@Param("userId") Long userId, @Param("cutoffDate") LocalDate cutoffDate, @Param("goalName") String goalName);

    // Methods for nudge detection
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId AND t.amountCents >= :thresholdCents AND t.date >= :since AND t.amountCents > 0 ORDER BY t.date DESC")
    List<Transaction> findByAccountUserIdAndAmountThresholdSince(@Param("userId") Long userId, @Param("thresholdCents") Long thresholdCents, @Param("since") LocalDate since);

    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId AND t.date >= :since AND t.amountCents > 0 AND " +
           "t.id NOT IN (SELECT gc.id FROM GoalContribution gc WHERE gc.goal.id = :goalId AND gc.description LIKE CONCAT('%ID: ', t.plaidTransactionId, '%')) " +
           "ORDER BY t.date DESC")
    List<Transaction> findRecentInflowsForContribution(@Param("userId") Long userId, @Param("goalId") Long goalId, @Param("since") LocalDate since);
}
