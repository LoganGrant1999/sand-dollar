package com.sanddollar.service.impl;

import com.sanddollar.dto.aibudget.FinancialSnapshotResponse.FinancialTotals;
import com.sanddollar.service.SpendingDataProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Collections;

@Component
@Primary
public class SpendingDataProviderFallback implements SpendingDataProvider {

  @Override
  public SnapshotDto getCurrentMonthSnapshot(Long userId, ZoneId zoneId) {
    return new SnapshotDto(
      "No data",
      BigDecimal.ZERO,
      Collections.emptyList(),
      // FinancialTotals(expenses, savings, netCashFlow)
      new FinancialTotals(
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO
      )
    );
  }
}
