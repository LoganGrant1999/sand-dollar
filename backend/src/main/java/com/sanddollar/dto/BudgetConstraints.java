package com.sanddollar.dto;

import java.time.LocalDate;

public record BudgetConstraints(
    String period, // "weekly", "biweekly", "monthly"
    LocalDate startDate,
    LocalDate endDate,
    Long maxSavingsTargetCents,
    String notes
) {}