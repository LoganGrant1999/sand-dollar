package com.sanddollar.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BudgetPlanSuggestion(
    Map<String, BigDecimal> categoryMonthlyCaps,
    BigDecimal suggestedMonthlyContribution,
    String strategy,
    List<String> rationale,
    boolean feasible,
    BigDecimal monthlyAvailable,
    BigDecimal monthlyRequired
) {}