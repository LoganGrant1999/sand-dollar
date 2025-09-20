package com.sanddollar.dto.budget;

import java.math.BigDecimal;

public record CategoryRow(
    String key,                  // normalized category name ("Groceries", etc.)
    BigDecimal amountMTD,        // this month so far (dollars)
    BigDecimal amountTypical,    // typical 3-mo for this category (dollars, 0 if N/A)
    String confidence            // "High" | "Medium" | "Low" (reuse existing logic)
) {}