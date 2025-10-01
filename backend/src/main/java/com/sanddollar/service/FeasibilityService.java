package com.sanddollar.service;

import com.sanddollar.dto.FeasibilityResult;
import com.sanddollar.entity.Goal;
import com.sanddollar.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FeasibilityService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SpendingService spendingService;

    public BigDecimal estimateMonthlyFreeCash(Long userId) {
        // Use Plaid-first methods from SpendingService
        BigDecimal incomeAvg = spendingService.estimateMonthlyIncomeFromPlaid(userId);
        BigDecimal fixedAvg = spendingService.estimateFixedMonthlyFromPlaid(userId);
        BigDecimal variableAvg = spendingService.estimateVariableMonthlyFromPlaid(userId);

        // Fallback to previous heuristics if any value is null
        if (incomeAvg == null || fixedAvg == null || variableAvg == null) {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(90);

            Long totalIncomeCents = transactionRepository.getTotalIncomeForUserInPeriod(userId, startDate, endDate);
            Long totalExpensesCents = transactionRepository.getTotalExpensesForUserInPeriod(userId, startDate, endDate);

            if (totalIncomeCents == null) totalIncomeCents = 0L;
            if (totalExpensesCents == null) totalExpensesCents = 0L;

            BigDecimal monthlyIncome = new BigDecimal(totalIncomeCents).divide(new BigDecimal(300), 2, RoundingMode.HALF_UP);
            BigDecimal monthlyExpenses = new BigDecimal(Math.abs(totalExpensesCents)).divide(new BigDecimal(300), 2, RoundingMode.HALF_UP);

            return monthlyIncome.subtract(monthlyExpenses).max(BigDecimal.ZERO);
        }

        // Calculate available cash using Plaid-first breakdown
        BigDecimal monthlyAvailable = incomeAvg.subtract(fixedAvg.add(variableAvg));
        return monthlyAvailable.max(BigDecimal.ZERO);
    }

    public FeasibilityResult plan(Long userId, Goal goal) {
        // Get baseline numbers using Plaid-first methods
        BigDecimal incomeAvg = spendingService.estimateMonthlyIncomeFromPlaid(userId);
        BigDecimal fixedAvg = spendingService.estimateFixedMonthlyFromPlaid(userId);
        BigDecimal variableAvg = spendingService.estimateVariableMonthlyFromPlaid(userId);

        BigDecimal monthlyAvailable = estimateMonthlyFreeCash(userId);

        // Calculate months between now and target date
        LocalDate now = LocalDate.now();
        int monthsToTarget = (int) ChronoUnit.MONTHS.between(now, goal.getTargetDate());
        if (monthsToTarget <= 0) {
            monthsToTarget = 1; // Minimum 1 month
        }

        // Calculate monthly required amount
        BigDecimal monthlyRequired = goal.getTargetAmount().divide(new BigDecimal(monthsToTarget), 2, RoundingMode.HALF_UP);

        // Determine verdict
        String verdict;
        String narrative;
        List<FeasibilityResult.SuggestedAdjustment> adjustments = new ArrayList<>();

        if (monthlyRequired.compareTo(monthlyAvailable) <= 0) {
            verdict = "YES";
            narrative = String.format("Great news! You can afford this goal. You need $%.2f per month and have $%.2f available.",
                monthlyRequired, monthlyAvailable);
        } else if (monthlyRequired.compareTo(monthlyAvailable.multiply(new BigDecimal("1.5"))) <= 0) {
            verdict = "MAYBE";
            narrative = String.format("This goal is challenging but possible with some budget adjustments. You need $%.2f per month but only have $%.2f available.",
                monthlyRequired, monthlyAvailable);

            // Suggest reducing variable expenses based on actual spending
            BigDecimal shortfall = monthlyRequired.subtract(monthlyAvailable);
            adjustments.add(new FeasibilityResult.SuggestedAdjustment(
                "Dining Out",
                shortfall.multiply(new BigDecimal("0.6")).negate(),
                "Reduce restaurant spending"
            ));
            adjustments.add(new FeasibilityResult.SuggestedAdjustment(
                "Entertainment",
                shortfall.multiply(new BigDecimal("0.4")).negate(),
                "Cut back on entertainment expenses"
            ));
        } else {
            verdict = "NO";
            narrative = String.format("This goal timeline is too aggressive. You need $%.2f per month but only have $%.2f available. Consider extending the timeline or reducing the target amount.",
                monthlyRequired, monthlyAvailable);

            // Suggest extending timeline
            int feasibleMonths = monthlyAvailable.compareTo(BigDecimal.ZERO) > 0
                ? goal.getTargetAmount().divide(monthlyAvailable, 0, RoundingMode.CEILING).intValue()
                : monthsToTarget * 2;

            adjustments.add(new FeasibilityResult.SuggestedAdjustment(
                "Timeline Extension",
                new BigDecimal(feasibleMonths - monthsToTarget),
                String.format("Extend timeline to %d months", feasibleMonths)
            ));
        }

        return new FeasibilityResult(
            monthlyRequired,
            monthlyAvailable,
            monthsToTarget,
            verdict,
            narrative,
            adjustments,
            incomeAvg != null ? incomeAvg : BigDecimal.ZERO,
            fixedAvg != null ? fixedAvg : BigDecimal.ZERO,
            variableAvg != null ? variableAvg : BigDecimal.ZERO
        );
    }
}