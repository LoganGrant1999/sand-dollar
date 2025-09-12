package com.sanddollar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanddollar.dto.BudgetConstraints;
import com.sanddollar.entity.*;
import com.sanddollar.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AIToolService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;
    
    @Autowired
    private BudgetPlanRepository budgetPlanRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private SpendingService spendingService;
    
    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> getUserSpendSummary(User user, String period) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = getStartDateForPeriod(endDate, period);
        
        List<Object[]> categoryData = transactionRepository.getSpendingByCategory(user, startDate, endDate);
        
        List<Map<String, Object>> categories = new ArrayList<>();
        long totalSpent = 0;
        
        for (Object[] row : categoryData) {
            String categoryTop = (String) row[0];
            Long totalCents = (Long) row[1];
            Integer txCount = ((Number) row[2]).intValue();
            
            totalSpent += totalCents;
            
            categories.add(Map.of(
                "categoryTop", categoryTop != null ? categoryTop : "Other",
                "totalCents", totalCents,
                "avgPerPeriodCents", totalCents, // For the given period
                "variance", 0.1, // Simplified - would need historical data
                "txCount", txCount
            ));
        }
        
        // Estimate income (simplified - look for positive large amounts)
        List<Transaction> transactions = transactionRepository
            .findByUserAndDateRange(user, startDate, endDate);
        
        long estimatedIncome = transactions.stream()
            .filter(t -> t.getAmountCents() > 0 && t.getAmountCents() > 50000) // $500+
            .mapToLong(Transaction::getAmountCents)
            .sum();
        
        return Map.of(
            "categories", categories,
            "totalSpentCents", totalSpent,
            "estimatedIncomeCents", estimatedIncome,
            "period", period,
            "startDate", startDate.toString(),
            "endDate", endDate.toString()
        );
    }

    public Object proposeBudgetPlan(User user, Map<String, Object> args, BudgetConstraints constraints) {
        try {
            Map<String, Object> goals = (Map<String, Object>) args.get("goals");
            
            // Extract goals
            Long targetCents = goals.containsKey("targetCents") ? 
                ((Number) goals.get("targetCents")).longValue() : 0L;
            String targetDateStr = (String) goals.get("targetDate");
            String notes = (String) goals.get("notes");
            
            LocalDate targetDate = targetDateStr != null ? 
                LocalDate.parse(targetDateStr) : LocalDate.now().plusMonths(3);
            
            // Determine period and dates
            String period = constraints != null && constraints.period() != null ? 
                constraints.period() : "monthly";
            LocalDate startDate = constraints != null && constraints.startDate() != null ?
                constraints.startDate() : LocalDate.now().withDayOfMonth(1).plusMonths(1);
            LocalDate endDate = targetDate;
            
            // Get spending data to create realistic budget
            Map<String, Object> spendData = getUserSpendSummary(user, "60d");
            List<Map<String, Object>> categories = (List<Map<String, Object>>) spendData.get("categories");
            
            // Create budget plan JSON
            List<Map<String, Object>> budgetTargets = new ArrayList<>();
            long totalBudgetCents = 0;
            
            for (Map<String, Object> category : categories) {
                String categoryName = (String) category.get("categoryTop");
                Long historicalSpend = (Long) category.get("totalCents");
                
                // Reduce spending by 10-20% to allow for savings
                Long budgetLimit = Math.round(historicalSpend * 0.85);
                totalBudgetCents += budgetLimit;
                
                budgetTargets.add(Map.of(
                    "categoryId", categoryName.toLowerCase().replace(" ", "_"),
                    "name", categoryName,
                    "limitCents", budgetLimit
                ));
            }
            
            // Validate savings target
            Long estimatedIncome = (Long) spendData.get("estimatedIncomeCents");
            Long maxSavings = Math.max(0, estimatedIncome - totalBudgetCents);
            Long savingsTarget = Math.min(targetCents, maxSavings);
            
            Map<String, Object> planJson = Map.of(
                "period", period,
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "targets", budgetTargets,
                "savingsTargetCents", savingsTarget,
                "notes", notes != null ? notes : "AI-generated budget plan"
            );
            
            // Save the budget plan
            BudgetPlan budgetPlan = new BudgetPlan(user, 
                BudgetPlan.BudgetPeriodType.valueOf(period.toUpperCase()),
                startDate, endDate, objectMapper.writeValueAsString(planJson));
            
            budgetPlan = budgetPlanRepository.save(budgetPlan);
            
            return Map.of(
                "planId", budgetPlan.getId(),
                "plan", planJson,
                "created", true,
                "message", "Budget plan created successfully"
            );
            
        } catch (Exception e) {
            return Map.of("error", "Failed to create budget plan: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBalances(User user) {
        Map<String, Object> balances = spendingService.getTotalBalance(user);
        return Map.of(
            "totalAvailableCents", balances.get("totalAvailableCents"),
            "asOf", balances.get("asOf").toString(),
            "currency", "USD"
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTransactions(User user, Map<String, Object> args) {
        try {
            Map<String, Object> range = (Map<String, Object>) args.get("range");
            String startDateStr = (String) range.get("startDate");
            String endDateStr = (String) range.get("endDate");
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            List<Transaction> transactions = transactionRepository
                .findSpendingByUserAndDateRange(user, startDate, endDate); // Excludes transfers by default
            
            List<Map<String, Object>> txnData = transactions.stream()
                .limit(50) // Limit to recent 50 for AI processing
                .map(t -> {
                    Map<String, Object> txnMap = new HashMap<>();
                    txnMap.put("date", t.getDate().toString());
                    txnMap.put("name", t.getName());
                    txnMap.put("merchant", t.getMerchantName() != null ? t.getMerchantName() : "");
                    txnMap.put("amountCents", t.getAmountCents());
                    txnMap.put("category", t.getCategoryTop() != null ? t.getCategoryTop() : "Other");
                    return txnMap;
                })
                .collect(Collectors.toList());
                
            return Map.of(
                "transactions", txnData,
                "count", txnData.size(),
                "period", startDate + " to " + endDate
            );
            
        } catch (Exception e) {
            return Map.of("error", "Failed to get transactions: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
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
                "planId", plan.getId(),
                "period", plan.getPeriod().toString().toLowerCase(),
                "startDate", plan.getStartDate().toString(),
                "endDate", plan.getEndDate().toString(),
                "plan", planData,
                "status", plan.getStatus().toString()
            );
            
        } catch (Exception e) {
            return Map.of("error", "Failed to parse budget plan: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGoalProgress(User user) {
        List<Goal> goals = goalRepository.findByUserAndStatus(user, Goal.GoalStatus.ACTIVE);
        
        List<Map<String, Object>> goalData = goals.stream()
            .map(g -> {
                double progress = g.getTargetCents() > 0 ? 
                    (double) g.getSavedCents() / g.getTargetCents() * 100 : 0;
                
                Map<String, Object> goalMap = new HashMap<>();
                goalMap.put("id", g.getId());
                goalMap.put("name", g.getName());
                goalMap.put("targetCents", g.getTargetCents());
                goalMap.put("savedCents", g.getSavedCents());
                goalMap.put("progressPercent", Math.round(progress * 100.0) / 100.0);
                goalMap.put("targetDate", g.getTargetDate() != null ? g.getTargetDate().toString() : null);
                goalMap.put("status", g.getStatus().toString());
                return goalMap;
            })
            .collect(Collectors.toList());
            
        return Map.of(
            "goals", goalData,
            "count", goalData.size()
        );
    }

    private LocalDate getStartDateForPeriod(LocalDate endDate, String period) {
        return switch (period) {
            case "30d" -> endDate.minusDays(30);
            case "60d" -> endDate.minusDays(60);
            case "90d" -> endDate.minusDays(90);
            default -> endDate.minusDays(30);
        };
    }
}