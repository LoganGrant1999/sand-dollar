package com.sanddollar.service;

import com.sanddollar.dto.aibudget.FinancialSnapshotResponse;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

public interface SpendingDataProvider {

    SnapshotDto getCurrentMonthSnapshot(Long userId, ZoneId zoneId);

    record SnapshotDto(
        String month,
        BigDecimal income,
        List<FinancialSnapshotResponse.CategoryActual> actualsByCategory,
        FinancialSnapshotResponse.FinancialTotals totals
    ) {}
}
