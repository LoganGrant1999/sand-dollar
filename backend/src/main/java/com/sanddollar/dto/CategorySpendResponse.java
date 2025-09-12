package com.sanddollar.dto;

import java.util.List;

public record CategorySpendResponse(
    List<CategorySpend> categories,
    Long totalSpentCents,
    String period
) {
    public record CategorySpend(
        String category,
        Long totalCents,
        Integer transactionCount,
        String trend, // "up", "down", "stable"
        Double trendPercentage
    ) {}
}