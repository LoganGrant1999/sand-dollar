package com.sanddollar.repository;

import com.sanddollar.entity.BudgetPlan;
import com.sanddollar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {
    List<BudgetPlan> findByUser(User user);
    List<BudgetPlan> findByUserAndStatus(User user, BudgetPlan.BudgetStatus status);
    Optional<BudgetPlan> findTopByUserAndStatusOrderByCreatedAtDesc(User user, BudgetPlan.BudgetStatus status);
}