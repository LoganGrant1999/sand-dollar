package com.sanddollar.dto;

import java.math.BigDecimal;
import java.util.List;

public record FeasibilityResult(
    BigDecimal monthlyRequired,
    BigDecimal monthlyAvailable,
    int monthsToTarget,
    String verdict, // "YES", "NO", "MAYBE"
    String narrative,
    List<SuggestedAdjustment> suggestedAdjustments,
    BigDecimal incomeAvg,
    BigDecimal fixedAvg,
    BigDecimal variableAvg
) {
    public record SuggestedAdjustment(
        String category,
        BigDecimal deltaMonthly,
        String impact
    ) {}
}