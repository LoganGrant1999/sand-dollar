package com.sanddollar.repository;

import com.sanddollar.entity.BudgetAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetAllocationRepository extends JpaRepository<BudgetAllocation, UUID> {
    
    List<BudgetAllocation> findByBudgetIdOrderByTypeAscCategoryAsc(UUID budgetId);
    
    void deleteByBudgetId(UUID budgetId);
}