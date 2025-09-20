package com.sanddollar.dto.budget;

import java.math.BigDecimal;
import java.util.List;

public record BudgetOverviewDTO(
    String monthIso,                // "2025-09"
    BigDecimal incomeMTD,           // dollars
    BigDecimal expensesMTD,
    BigDecimal netMTD,
    double savingsRateMTD,          // -1.01 == -101%
    BigDecimal incomeTypical,       // 3-mo winsorized mean or median fallback
    BigDecimal expensesTypical,
    BigDecimal netTypical,
    double savingsRateTypical,
    List<CategoryRow> categoriesMTD // sorted desc by spend
) {}