package com.sanddollar.controller;

import com.sanddollar.budgeting.BudgetBaselineService;
import com.sanddollar.entity.User;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/budget")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BudgetBaselineService budgetBaselineService;

    @GetMapping("/active")
    public ResponseEntity<?> getActiveBudget(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            Map<String, Object> budget = budgetService.getActiveBudget(user);
            return ResponseEntity.ok(budget);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get active budget: " + e.getMessage()));
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getBudgetProgress(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            Map<String, Object> progress = budgetService.getBudgetProgress(user);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get budget progress: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getBudgetHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "6") int limit) {
        try {
            User user = userPrincipal.getUser();
            var history = budgetService.getBudgetHistory(user, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get budget history: " + e.getMessage()));
        }
    }

    @GetMapping("/baseline")
    public ResponseEntity<?> getBudgetBaseline(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            BudgetBaselineService.BudgetBaseline baseline = budgetBaselineService.calculateBaseline(user);

            Map<String, Object> response = Map.of(
                "monthlyIncome", convertCentsToDollars(baseline.getMonthlyIncomeCents()),
                "totalMonthlyExpenses", convertCentsToDollars(baseline.getTotalMonthlyExpensesCents()),
                "monthlyExpensesByCategory", baseline.getMonthlyExpensesByCategory()
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertCentsToDollars(entry.getValue())
                    )),
                "paycheckCadence", baseline.getPaycheckCadence().toString(),
                "categoryConfidenceScores", baseline.getCategoryConfidenceScores()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to calculate budget baseline: " + e.getMessage()));
        }
    }

    private BigDecimal convertCentsToDollars(long cents) {
        return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}