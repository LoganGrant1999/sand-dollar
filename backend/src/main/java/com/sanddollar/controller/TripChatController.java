package com.sanddollar.controller;

import com.sanddollar.dto.FeasibilityResult;
import com.sanddollar.dto.BudgetPlanSuggestion;
import com.sanddollar.entity.Goal;
import com.sanddollar.entity.User;
import com.sanddollar.repository.PlaidItemRepository;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.FeasibilityService;
import com.sanddollar.service.BudgetPlanService;
import com.sanddollar.service.SpendingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trip-chat")
public class TripChatController {

    @Autowired
    private FeasibilityService feasibilityService;

    @Autowired
    private BudgetPlanService budgetPlanService;

    @Autowired
    private SpendingService spendingService;

    @Autowired
    private PlaidItemRepository plaidItemRepository;

    @PostMapping("/finalize")
    public ResponseEntity<Map<String, Object>> finalize(
            @Valid @RequestBody TripCostEstimateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // Build TripCostEstimate from request
            TripCostEstimate estimate = new TripCostEstimate(
                request.destination(),
                request.totalCost(),
                request.targetDate(),
                request.breakdown()
            );

            // Create a temporary Goal for feasibility analysis
            Goal tempGoal = new Goal();
            tempGoal.setTargetAmount(request.totalCost());
            tempGoal.setTargetDate(request.targetDate());
            tempGoal.setName("Trip to " + request.destination());

            // Check if user has Plaid connection and income data
            boolean hasPlaidConnection = plaidItemRepository.findByUserAndStatus(
                getUserFromPrincipal(userPrincipal),
                com.sanddollar.entity.PlaidItem.PlaidItemStatus.ACTIVE
            ).size() > 0;

            BigDecimal incomeAvg = spendingService.estimateMonthlyIncomeFromPlaid(userPrincipal.getUserId());
            boolean hasIncomeData = incomeAvg != null && incomeAvg.compareTo(BigDecimal.ZERO) > 0;

            // Determine if provisional
            boolean provisional = !hasPlaidConnection || !hasIncomeData;
            String provisionalMessage = provisional ? "Link accounts to verify and auto-tune this plan." : null;

            // Get feasibility analysis
            FeasibilityResult feasibility = feasibilityService.plan(userPrincipal.getUserId(), tempGoal);

            // Get budget plan suggestion
            BudgetPlanSuggestion budgetPlan = budgetPlanService.buildBudgetPlan(
                userPrincipal.getUserId(), tempGoal, feasibility
            );

            // Get baseline numbers
            Map<String, Object> baselines = new HashMap<>();
            baselines.put("incomeAvg", feasibility.incomeAvg());
            baselines.put("fixedAvg", feasibility.fixedAvg());
            baselines.put("variableAvg", feasibility.variableAvg());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("brief", request.brief());
            response.put("estimate", estimate);
            response.put("feasibility", feasibility);
            response.put("budgetPlan", budgetPlan);
            response.put("baselines", baselines);
            response.put("provisional", provisional);

            if (provisional && provisionalMessage != null) {
                response.put("message", provisionalMessage);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private User getUserFromPrincipal(UserPrincipal userPrincipal) {
        User user = new User();
        user.setId(userPrincipal.getUserId());
        return user;
    }

    // Request DTO
    public record TripCostEstimateRequest(
        String brief,
        String destination,
        BigDecimal totalCost,
        LocalDate targetDate,
        Map<String, BigDecimal> breakdown
    ) {}

    // Response DTO
    public record TripCostEstimate(
        String destination,
        BigDecimal totalCost,
        LocalDate targetDate,
        Map<String, BigDecimal> breakdown
    ) {}
}