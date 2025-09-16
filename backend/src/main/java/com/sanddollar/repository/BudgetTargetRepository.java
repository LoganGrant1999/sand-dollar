package com.sanddollar.repository;

import com.sanddollar.entity.BudgetTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetTargetRepository extends JpaRepository<BudgetTarget, Long> {
    
    /**
     * Find all budget targets for a user in a specific month
     */
    List<BudgetTarget> findByUserIdAndMonthOrderByCategory(Long userId, String month);
    
    /**
     * Find a specific budget target for user/month/category
     */
    Optional<BudgetTarget> findByUserIdAndMonthAndCategory(Long userId, String month, String category);
    
    /**
     * Delete all targets for a user in a specific month
     * Used when accepting new AI budget to replace existing
     */
    @Modifying
    @Query("DELETE FROM BudgetTarget bt WHERE bt.userId = :userId AND bt.month = :month")
    void deleteByUserIdAndMonth(@Param("userId") Long userId, @Param("month") String month);
    
    /**
     * Find all targets for a user across multiple months for historical analysis
     */
    @Query("SELECT bt FROM BudgetTarget bt WHERE bt.userId = :userId AND bt.month IN :months ORDER BY bt.month DESC, bt.category")
    List<BudgetTarget> findByUserIdAndMonthInOrderByMonthDescCategoryAsc(@Param("userId") Long userId, @Param("months") List<String> months);
}