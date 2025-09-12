package com.sanddollar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanddollar.entity.BudgetPlan;
import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.User;
import com.sanddollar.repository.BudgetPlanRepository;
import com.sanddollar.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class BudgetService {

    @Autowired
    private BudgetPlanRepository budgetPlanRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> getActiveBudget(User user) {
        Optional<BudgetPlan> activePlan = budgetPlanRepository
            .findTopByUserAndStatusOrderByCreatedAtDesc(user, BudgetPlan.BudgetStatus.ACTIVE);
        
        if (activePlan.isEmpty()) {
            return Map.of("message", "No active budget plan found");
        }
        
        try {
            BudgetPlan plan = activePlan.get();
            Map<String, Object> planData = objectMapper.readValue(plan.getPlanJson(), Map.class);
            
            return Map.of(
                "id", plan.getId(),
                "period", plan.getPeriod().toString().toLowerCase(),
                "startDate", plan.getStartDate().toString(),
                "endDate", plan.getEndDate().toString(),
                "plan", planData,
                "status", plan.getStatus().toString(),
                "createdAt", plan.getCreatedAt().toString()
            );
            
        } catch (Exception e) {
            return Map.of("error", "Failed to parse budget plan: " + e.getMessage());
        }
    }

    public Map<String, Object> getBudgetProgress(User user) {
        Optional<BudgetPlan> activePlan = budgetPlanRepository
            .findTopByUserAndStatusOrderByCreatedAtDesc(user, BudgetPlan.BudgetStatus.ACTIVE);
        
        if (activePlan.isEmpty()) {
            return Map.of("message", "No active budget plan found");
        }
        
        try {
            BudgetPlan plan = activePlan.get();
            Map<String, Object> planData = objectMapper.readValue(plan.getPlanJson(), Map.class);
            List<Map<String, Object>> targets = (List<Map<String, Object>>) planData.get("targets");
            
            // Get current period dates
            LocalDate startDate = plan.getStartDate();
            LocalDate endDate = LocalDate.now().isBefore(plan.getEndDate()) ? 
                LocalDate.now() : plan.getEndDate();
            
            // Get actual spending by category for current period
            Map<String, Long> actualSpending = getActualSpendingByCategory(user, startDate, endDate);
            
            List<Map<String, Object>> progress = new ArrayList<>();
            
            for (Map<String, Object> target : targets) {
                String categoryName = (String) target.get("name");
                Long limitCents = ((Number) target.get("limitCents")).longValue();
                Long spentCents = actualSpending.getOrDefault(categoryName, 0L);
                
                double percentage = limitCents > 0 ? (double) spentCents / limitCents * 100 : 0;
                String status = getSpendingStatus(percentage);
                
                progress.add(Map.of(
                    "categoryTop", categoryName,
                    "limitCents", limitCents,
                    "spentCents", spentCents,
                    "percentage", Math.round(percentage * 100.0) / 100.0,
                    "status", status,
                    "remainingCents", Math.max(0, limitCents - spentCents)
                ));
            }
            
            // Calculate overall progress
            long totalBudget = targets.stream()
                .mapToLong(t -> ((Number) t.get("limitCents")).longValue())
                .sum();
            long totalSpent = actualSpending.values().stream()
                .mapToLong(Long::longValue)
                .sum();
            
            return Map.of(
                "planId", plan.getId(),
                "period", plan.getPeriod().toString().toLowerCase(),
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "categories", progress,
                "totalBudgetCents", totalBudget,
                "totalSpentCents", totalSpent,
                "overallPercentage", totalBudget > 0 ? 
                    Math.round((double) totalSpent / totalBudget * 10000.0) / 100.0 : 0
            );
            
        } catch (Exception e) {
            return Map.of("error", "Failed to calculate budget progress: " + e.getMessage());
        }
    }

    private Map<String, Long> getActualSpendingByCategory(User user, LocalDate startDate, LocalDate endDate) {
        List<Object[]> categoryData = transactionRepository.getSpendingByCategory(user, startDate, endDate);
        
        Map<String, Long> spending = new HashMap<>();
        for (Object[] row : categoryData) {
            String category = (String) row[0];
            Long amount = (Long) row[1];
            spending.put(category != null ? category : "Other", amount);
        }
        
        return spending;
    }

    private String getSpendingStatus(double percentage) {
        if (percentage >= 100) {
            return "over_budget";
        } else if (percentage >= 80) {
            return "warning";
        } else {
            return "on_track";
        }
    }
}