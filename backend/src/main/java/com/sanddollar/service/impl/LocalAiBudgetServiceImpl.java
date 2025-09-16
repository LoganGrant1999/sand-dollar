package com.sanddollar.service.impl;

import com.sanddollar.dto.aibudget.*;
import com.sanddollar.entity.BudgetTarget;
import com.sanddollar.repository.BudgetTargetRepository;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.service.AiBudgetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("local")
public class LocalAiBudgetServiceImpl implements AiBudgetService {

    private static final Logger logger = LoggerFactory.getLogger(LocalAiBudgetServiceImpl.class);
    private static final Long TEST_USER_ID = 9001L;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetTargetRepository budgetTargetRepository;

    @Value("${app.aiBudget.mockEnabled:true}")
    private boolean mockEnabled;

    @Override
    public FinancialSnapshotResponse getFinancialSnapshot() {
        if (!mockEnabled) {
            throw new IllegalStateException("Local AI budget service requires mockEnabled=true");
        }

        logger.info("Generating financial snapshot for local profile using test user ID: {}", TEST_USER_ID);

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        // Get actual spending data from seeded transactions
        List<FinancialSnapshotResponse.CategoryActual> actualsByCategory = getActualSpendingByCategory(startOfMonth, endOfMonth);

        // Calculate totals
        BigDecimal totalExpenses = actualsByCategory.stream()
            .map(FinancialSnapshotResponse.CategoryActual::getActual)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get income data
        Long incomeRaw = transactionRepository.sumIncomeByUserIdAndDateRange(TEST_USER_ID, startOfMonth, endOfMonth);
        BigDecimal income = incomeRaw != null ? new BigDecimal(incomeRaw).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP) : new BigDecimal("6200.00");

        // Calculate savings and net cash flow
        BigDecimal savings = income.subtract(totalExpenses);
        BigDecimal netCashFlow = savings;

        FinancialSnapshotResponse.FinancialTotals totals = new FinancialSnapshotResponse.FinancialTotals(
            totalExpenses,
            savings.max(BigDecimal.ZERO),
            netCashFlow
        );

        // Get existing budget targets for this month if they exist
        List<FinancialSnapshotResponse.CategoryTarget> targetsByCategory = getBudgetTargets(currentMonth);
        Instant acceptedAt = targetsByCategory.isEmpty() ? null : Instant.now();

        return new FinancialSnapshotResponse(
            currentMonth,
            income,
            actualsByCategory,
            totals,
            targetsByCategory,
            acceptedAt
        );
    }

    private List<FinancialSnapshotResponse.CategoryActual> getActualSpendingByCategory(LocalDate startDate, LocalDate endDate) {
        // Get spending by category from the database
        List<Object[]> spendingData = transactionRepository.getSpendingByCategory(null, startDate, endDate);

        // If no data found in DB (user entity is null), use hardcoded realistic data based on seeded transactions
        if (spendingData.isEmpty()) {
            return Arrays.asList(
                new FinancialSnapshotResponse.CategoryActual("Rent", new BigDecimal("1500.00")),
                new FinancialSnapshotResponse.CategoryActual("Groceries", new BigDecimal("420.50")),
                new FinancialSnapshotResponse.CategoryActual("Dining", new BigDecimal("360.75")),
                new FinancialSnapshotResponse.CategoryActual("Transport", new BigDecimal("120.00")),
                new FinancialSnapshotResponse.CategoryActual("Utilities", new BigDecimal("160.00")),
                new FinancialSnapshotResponse.CategoryActual("Gym", new BigDecimal("40.00")),
                new FinancialSnapshotResponse.CategoryActual("Subscriptions", new BigDecimal("70.30")),
                new FinancialSnapshotResponse.CategoryActual("Misc", new BigDecimal("130.20"))
            );
        }

        return spendingData.stream()
            .map(row -> {
                String category = (String) row[0];
                Long amountCents = ((Number) row[1]).longValue();
                BigDecimal amount = new BigDecimal(amountCents).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                return new FinancialSnapshotResponse.CategoryActual(category, amount);
            })
            .collect(Collectors.toList());
    }

    private List<FinancialSnapshotResponse.CategoryTarget> getBudgetTargets(String month) {
        List<BudgetTarget> targets = budgetTargetRepository.findByUserIdAndMonthOrderByCategory(TEST_USER_ID, month);

        return targets.stream()
            .map(target -> new FinancialSnapshotResponse.CategoryTarget(
                target.getCategory(),
                new BigDecimal(target.getTargetCents()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP),
                target.getReason()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public GenerateBudgetResponse generateBudget(GenerateBudgetRequest request) {
        if (!mockEnabled) {
            throw new IllegalStateException("Local AI budget service requires mockEnabled=true");
        }

        logger.info("Generating deterministic budget plan for local profile for month: {}", request.getMonth());

        // Return a deterministic budget plan based on the requirements
        List<GenerateBudgetResponse.CategoryTarget> targets = Arrays.asList(
            new GenerateBudgetResponse.CategoryTarget("Rent", new BigDecimal("1500.00"), "Fixed obligation"),
            new GenerateBudgetResponse.CategoryTarget("Groceries", new BigDecimal("380.00"), "Based on last-90-day avg −10%"),
            new GenerateBudgetResponse.CategoryTarget("Dining", new BigDecimal("300.00"), "User cap, weekends out"),
            new GenerateBudgetResponse.CategoryTarget("Transport", new BigDecimal("140.00"), "Fuel + 2 rideshares"),
            new GenerateBudgetResponse.CategoryTarget("Utilities", new BigDecimal("160.00"), "Electric + water + internet"),
            new GenerateBudgetResponse.CategoryTarget("Gym", new BigDecimal("40.00"), "Maintain routine"),
            new GenerateBudgetResponse.CategoryTarget("Subscriptions", new BigDecimal("70.00"), "Audit next month"),
            new GenerateBudgetResponse.CategoryTarget("Misc", new BigDecimal("120.00"), "Small surprises buffer"),
            new GenerateBudgetResponse.CategoryTarget("Emergency Fund", new BigDecimal("800.00"), "Pace to $5k by March"),
            new GenerateBudgetResponse.CategoryTarget("Debt Paydown", new BigDecimal("200.00"), "Reduce card monthly")
        );

        GenerateBudgetResponse.BudgetSummary summary = new GenerateBudgetResponse.BudgetSummary(
            new BigDecimal("0.20"), // 20% savings rate
            Arrays.asList(
                "Reduced discretionary by ~8%",
                "Prioritized EF + debt"
            )
        );

        return new GenerateBudgetResponse(
            request.getMonth() != null ? request.getMonth() : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
            targets,
            summary,
            0, // promptTokens - no OpenAI call for local
            0  // completionTokens - no OpenAI call for local
        );
    }

    @Override
    @Transactional
    public AcceptBudgetResponse acceptBudget(AcceptBudgetRequest request) {
        if (!mockEnabled) {
            throw new IllegalStateException("Local AI budget service requires mockEnabled=true");
        }

        logger.info("Accepting budget targets for local profile for month: {} with {} categories",
            request.getMonth(), request.getTargetsByCategory().size());

        String month = request.getMonth();

        // Delete existing targets for this month
        budgetTargetRepository.deleteByUserIdAndMonth(TEST_USER_ID, month);

        // Create new targets
        List<BudgetTarget> newTargets = request.getTargetsByCategory().stream()
            .map(target -> {
                int targetCents = target.getTarget().multiply(new BigDecimal(100)).intValue();
                return new BudgetTarget(TEST_USER_ID, month, target.getCategory(), targetCents, target.getReason());
            })
            .collect(Collectors.toList());

        budgetTargetRepository.saveAll(newTargets);

        logger.info("Successfully saved {} budget targets for user {} in month {}",
            newTargets.size(), TEST_USER_ID, month);

        return new AcceptBudgetResponse("accepted");
    }
}