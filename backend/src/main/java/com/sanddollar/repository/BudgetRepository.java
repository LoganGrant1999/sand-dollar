package com.sanddollar.repository;

import com.sanddollar.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    
    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.allocations WHERE b.userId = :userId ORDER BY b.year DESC, b.month DESC")
    List<Budget> findByUserIdWithAllocationsOrderByDateDesc(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.allocations WHERE b.userId = :userId ORDER BY b.year DESC, b.month DESC LIMIT 1")
    Optional<Budget> findCurrentBudgetByUserId(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.allocations WHERE b.id = :id")
    Optional<Budget> findByIdWithAllocations(@Param("id") UUID id);
    
    Optional<Budget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);
    
    List<Budget> findByUserIdOrderByYearDescMonthDesc(Long userId);
}