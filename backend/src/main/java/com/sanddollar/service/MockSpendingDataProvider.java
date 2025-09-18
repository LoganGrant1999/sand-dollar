package com.sanddollar.service;

import com.sanddollar.dto.aibudget.FinancialSnapshotResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@ConditionalOnMissingBean(SpendingDataProvider.class)
public class MockSpendingDataProvider implements SpendingDataProvider {

    @Override
    public SnapshotDto getCurrentMonthSnapshot(Long userId, ZoneId zoneId) {
        ZoneId effectiveZone = zoneId != null ? zoneId : ZoneId.of("America/Denver");
        ZonedDateTime now = ZonedDateTime.now(effectiveZone);
        String month = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<FinancialSnapshotResponse.CategoryActual> actuals = Arrays.asList(
            new FinancialSnapshotResponse.CategoryActual("Rent", new BigDecimal("1500.00")),
            new FinancialSnapshotResponse.CategoryActual("Groceries", new BigDecimal("420.50")),
            new FinancialSnapshotResponse.CategoryActual("Dining", new BigDecimal("360.75")),
            new FinancialSnapshotResponse.CategoryActual("Transport", new BigDecimal("120.00")),
            new FinancialSnapshotResponse.CategoryActual("Utilities", new BigDecimal("160.00")),
            new FinancialSnapshotResponse.CategoryActual("Gym", new BigDecimal("40.00")),
            new FinancialSnapshotResponse.CategoryActual("Subscriptions", new BigDecimal("70.30")),
            new FinancialSnapshotResponse.CategoryActual("Misc", new BigDecimal("130.20"))
        );

        BigDecimal expenses = actuals.stream()
            .map(FinancialSnapshotResponse.CategoryActual::getActual)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal income = new BigDecimal("6200.00");
        BigDecimal savings = income.subtract(expenses);
        FinancialSnapshotResponse.FinancialTotals totals = new FinancialSnapshotResponse.FinancialTotals(
            expenses,
            savings.max(BigDecimal.ZERO),
            savings
        );

        return new SnapshotDto(month, income, actuals, totals);
    }
}
