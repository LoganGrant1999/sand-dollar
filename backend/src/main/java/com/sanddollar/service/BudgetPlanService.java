package com.sanddollar.service;

import com.sanddollar.dto.BudgetPlanSuggestion;
import com.sanddollar.dto.FeasibilityResult;
import com.sanddollar.entity.Goal;
import com.sanddollar.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class BudgetPlanService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FeasibilityService feasibilityService;

    @Autowired
    private SpendingService spendingService;

    public BudgetPlanSuggestion buildBudgetPlan(Long userId, Goal goal, FeasibilityResult feasibility) {
        // Seed categoryMonthlyCaps from Plaid category averages
        Map<String, BigDecimal> categoryAverages = spendingService.getCategoryAveragesFromPlaid(userId);
        Map<String, BigDecimal> categoryMonthlyCaps = new HashMap<>(categoryAverages);

        boolean feasible = "YES".equals(feasibility.verdict());
        BigDecimal suggestedContribution = feasibility.monthlyRequired();
        String strategy;
        List<String> rationale = new ArrayList<>();

        // Add baseline information to rationale
        if (feasibility.incomeAvg() != null && feasibility.fixedAvg() != null && feasibility.variableAvg() != null) {
            rationale.add(String.format("Monthly income avg: $%.2f, Fixed expenses: $%.2f, Variable expenses: $%.2f",
                feasibility.incomeAvg(), feasibility.fixedAvg(), feasibility.variableAvg()));
        }

        if (feasible) {
            strategy = "MAINTAIN_CURRENT";
            rationale.add("Your current spending allows for this goal");
            rationale.add(String.format("Continue current budget and save $%.2f monthly", feasibility.monthlyRequired()));
        } else if ("MAYBE".equals(feasibility.verdict())) {
            strategy = "MODERATE_CUTS";
            feasible = true; // Mark as feasible with adjustments

            BigDecimal shortfall = feasibility.monthlyRequired().subtract(feasibility.monthlyAvailable());

            // Prefer trims in order: Dining, Entertainment, Misc, then RideShare/Travel (not Groceries/Housing)
            // Cap trims at 20% per category
            adjustCategoryWithLimit(categoryMonthlyCaps, "RESTAURANTS", shortfall.multiply(new BigDecimal("0.40")), 0.20, rationale);
            adjustCategoryWithLimit(categoryMonthlyCaps, "ENTERTAINMENT", shortfall.multiply(new BigDecimal("0.30")), 0.20, rationale);
            adjustCategoryWithLimit(categoryMonthlyCaps, "OTHER", shortfall.multiply(new BigDecimal("0.20")), 0.20, rationale);
            adjustCategoryWithLimit(categoryMonthlyCaps, "TRANSPORTATION", shortfall.multiply(new BigDecimal("0.10")), 0.20, rationale);

            rationale.add(0, "Goal achievable with minor spending adjustments");
        } else {
            strategy = "EXTEND_TIMELINE";

            // Calculate what's feasible with current budget
            if (feasibility.monthlyAvailable().compareTo(BigDecimal.ZERO) > 0) {
                suggestedContribution = feasibility.monthlyAvailable().multiply(new BigDecimal("0.8"));
                feasible = true;
            }

            rationale.add("Current timeline too aggressive - consider extending target date");
            rationale.add(String.format("Recommended monthly contribution: $%.2f", suggestedContribution));
        }

        return new BudgetPlanSuggestion(
            categoryMonthlyCaps,
            suggestedContribution,
            strategy,
            rationale,
            feasible,
            feasibility.monthlyAvailable(),
            feasibility.monthlyRequired()
        );
    }

    // Replaced with SpendingService.getCategoryAveragesFromPlaid()

    private void adjustCategoryWithLimit(Map<String, BigDecimal> categoryMonthlyCaps, String category,
                                       BigDecimal reductionAmount, double maxReductionPercent, List<String> rationale) {
        BigDecimal currentAmount = categoryMonthlyCaps.get(category);
        if (currentAmount != null && currentAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Cap reduction at specified percentage
            BigDecimal maxReduction = currentAmount.multiply(new BigDecimal(maxReductionPercent));
            BigDecimal actualReduction = reductionAmount.min(maxReduction);

            BigDecimal newAmount = currentAmount.subtract(actualReduction).max(BigDecimal.ZERO);
            categoryMonthlyCaps.put(category, newAmount);

            // Include real baseline values in rationale
            rationale.add(String.format("%s avg $%.2f → cap $%.2f (-$%.2f)",
                category.toLowerCase(), currentAmount, newAmount, actualReduction));
        }
    }
}