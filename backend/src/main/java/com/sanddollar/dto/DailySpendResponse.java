package com.sanddollar.dto;

import java.time.LocalDate;
import java.util.List;

public record DailySpendResponse(
    List<DaySpend> dailySpend,
    Long totalSpentCents
) {
    public record DaySpend(
        LocalDate date,
        Long spentCents
    ) {}
}