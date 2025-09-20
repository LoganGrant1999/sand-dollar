package com.sanddollar.service.impl;

import com.sanddollar.dto.aibudget.*;
import com.sanddollar.service.AiBudgetService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@Profile("mock")
public class MockAiBudgetServiceImpl implements AiBudgetService {
    
    @Override
    public FinancialSnapshotResponse getFinancialSnapshot() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        List<FinancialSnapshotResponse.CategoryActual> actuals = Arrays.asList(
            new FinancialSnapshotResponse.CategoryActual("Rent", new BigDecimal("1500.00"), new BigDecimal("1500.00")),
            new FinancialSnapshotResponse.CategoryActual("Groceries", new BigDecimal("420.50"), new BigDecimal("380.00")),
            new FinancialSnapshotResponse.CategoryActual("Dining", new BigDecimal("280.75"), new BigDecimal("300.00")),
            new FinancialSnapshotResponse.CategoryActual("Transportation", new BigDecimal("180.00"), new BigDecimal("160.00")),
            new FinancialSnapshotResponse.CategoryActual("Utilities", new BigDecimal("150.00"), new BigDecimal("150.00")),
            new FinancialSnapshotResponse.CategoryActual("Entertainment", new BigDecimal("220.30"), new BigDecimal("200.00")),
            new FinancialSnapshotResponse.CategoryActual("Insurance", new BigDecimal("125.00"), new BigDecimal("125.00")),
            new FinancialSnapshotResponse.CategoryActual("Gas", new BigDecimal("160.45"), new BigDecimal("140.00")),
            new FinancialSnapshotResponse.CategoryActual("Shopping", new BigDecimal("340.20"), new BigDecimal("250.00"))
        );
        
        FinancialSnapshotResponse.FinancialTotals totals = new FinancialSnapshotResponse.FinancialTotals(
            new BigDecimal("3376.20"), // expenses
            new BigDecimal("823.80"),  // savings
            new BigDecimal("2000.00")  // net cash flow
        );
        
        List<FinancialSnapshotResponse.CategoryTarget> targets = Arrays.asList(
            new FinancialSnapshotResponse.CategoryTarget("Rent", new BigDecimal("1500.00"), "Fixed obligation"),
            new FinancialSnapshotResponse.CategoryTarget("Groceries", new BigDecimal("380.00"), "Based on past 3 mo avg −10%"),
            new FinancialSnapshotResponse.CategoryTarget("Dining", new BigDecimal("300.00"), "User constraint"),
            new FinancialSnapshotResponse.CategoryTarget("Emergency Fund", new BigDecimal("800.00"), "Goal pacing for $5k by March"),
            new FinancialSnapshotResponse.CategoryTarget("Debt Payment", new BigDecimal("200.00"), "Card balance reduction goal")
        );

        return new FinancialSnapshotResponse(
            currentMonth,
            new BigDecimal("6200.00"), // income
            actuals,
            totals,
            targets,
            java.time.Instant.now()
        );
    }
    
    @Override
    public GenerateBudgetResponse generateBudget(GenerateBudgetRequest request) {
        List<GenerateBudgetResponse.CategoryTarget> targets = Arrays.asList(
            new GenerateBudgetResponse.CategoryTarget("Rent", new BigDecimal("1500.00"), "Fixed obligation"),
            new GenerateBudgetResponse.CategoryTarget("Groceries", new BigDecimal("380.00"), "Based on past 3 mo avg −10%"),
            new GenerateBudgetResponse.CategoryTarget("Dining", new BigDecimal("300.00"), "User constraint"),
            new GenerateBudgetResponse.CategoryTarget("Transportation", new BigDecimal("160.00"), "Reduced by optimizing routes"),
            new GenerateBudgetResponse.CategoryTarget("Utilities", new BigDecimal("150.00"), "Fixed monthly average"),
            new GenerateBudgetResponse.CategoryTarget("Entertainment", new BigDecimal("200.00"), "Reduced for savings goal"),
            new GenerateBudgetResponse.CategoryTarget("Insurance", new BigDecimal("125.00"), "Fixed obligation"),
            new GenerateBudgetResponse.CategoryTarget("Gas", new BigDecimal("140.00"), "Optimized driving habits"),
            new GenerateBudgetResponse.CategoryTarget("Shopping", new BigDecimal("250.00"), "Reduced discretionary spending"),
            new GenerateBudgetResponse.CategoryTarget("Emergency Fund", new BigDecimal("800.00"), "Goal pacing for $5k by March"),
            new GenerateBudgetResponse.CategoryTarget("Debt Payment", new BigDecimal("200.00"), "Card balance reduction goal")
        );
        
        GenerateBudgetResponse.BudgetSummary summary = new GenerateBudgetResponse.BudgetSummary(
            new BigDecimal("0.20"), // 20% savings rate
            Arrays.asList(
                "Reduced discretionary spending by ~8%",
                "Optimized transportation costs",
                "On track for emergency fund goal"
            )
        );
        
        return new GenerateBudgetResponse(
            request.getMonth(),
            targets,
            summary,
            1250, // prompt tokens
            850   // completion tokens
        );
    }
    
    @Override
    public AcceptBudgetResponse acceptBudget(AcceptBudgetRequest request) {
        // In real implementation, would persist to database
        // For now, just return success
        return new AcceptBudgetResponse("ok");
    }
}
