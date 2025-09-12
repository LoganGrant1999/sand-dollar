package com.sanddollar.service;

import com.sanddollar.dto.CategorySpendResponse;
import com.sanddollar.dto.DailySpendResponse;
import com.sanddollar.entity.User;
import com.sanddollar.repository.BalanceSnapshotRepository;
import com.sanddollar.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SpendingService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;

    public Map<String, Object> getTotalBalance(User user) {
        Long totalAvailable = balanceSnapshotRepository.getTotalAvailableBalanceForUser(user.getId());
        if (totalAvailable == null) {
            totalAvailable = 0L;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("balance", totalAvailable / 100.0); // Convert cents to dollars
        result.put("totalAvailableCents", totalAvailable);
        result.put("asOf", Instant.now());
        return result;
    }

    public DailySpendResponse getDailySpending(User user, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Object[]> dailySpendData = transactionRepository.getDailySpending(user, startDate, endDate);
        
        Map<LocalDate, Long> spendByDate = new HashMap<>();
        for (Object[] row : dailySpendData) {
            LocalDate date = (LocalDate) row[0];
            Long amount = (Long) row[1];
            spendByDate.put(date, amount);
        }

        List<DailySpendResponse.DaySpend> dailySpend = new ArrayList<>();
        long totalSpent = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Long spent = spendByDate.getOrDefault(date, 0L);
            dailySpend.add(new DailySpendResponse.DaySpend(date, spent));
            totalSpent += spent;
        }

        return new DailySpendResponse(dailySpend, totalSpent);
    }

    public CategorySpendResponse getCategorySpending(User user, String period) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = getStartDateForPeriod(endDate, period);
        LocalDate previousStartDate = getStartDateForPeriod(startDate.minusDays(1), period);

        List<Object[]> currentPeriodData = transactionRepository.getSpendingByCategory(user, startDate, endDate);
        List<Object[]> previousPeriodData = transactionRepository.getSpendingByCategory(user, previousStartDate, startDate.minusDays(1));

        Map<String, Long> previousSpending = new HashMap<>();
        for (Object[] row : previousPeriodData) {
            String category = (String) row[0];
            Long amount = (Long) row[1];
            previousSpending.put(category, amount);
        }

        List<CategorySpendResponse.CategorySpend> categories = new ArrayList<>();
        long totalSpent = 0;

        for (Object[] row : currentPeriodData) {
            String category = (String) row[0];
            Long currentAmount = (Long) row[1];
            Integer count = ((Number) row[2]).intValue();
            
            totalSpent += currentAmount;

            Long previousAmount = previousSpending.getOrDefault(category, 0L);
            String trend = "stable";
            double trendPercentage = 0.0;

            if (previousAmount > 0) {
                trendPercentage = ((double) currentAmount - previousAmount) / previousAmount * 100;
                if (trendPercentage > 5) {
                    trend = "up";
                } else if (trendPercentage < -5) {
                    trend = "down";
                }
            } else if (currentAmount > 0) {
                trend = "up";
                trendPercentage = 100.0;
            }

            categories.add(new CategorySpendResponse.CategorySpend(
                category != null ? category : "Other",
                currentAmount,
                count,
                trend,
                Math.round(trendPercentage * 100.0) / 100.0
            ));
        }

        return new CategorySpendResponse(categories, totalSpent, period);
    }

    private LocalDate getStartDateForPeriod(LocalDate endDate, String period) {
        return switch (period) {
            case "30d" -> endDate.minusDays(30);
            case "60d" -> endDate.minusDays(60);
            case "90d" -> endDate.minusDays(90);
            default -> endDate.minusDays(30);
        };
    }
}