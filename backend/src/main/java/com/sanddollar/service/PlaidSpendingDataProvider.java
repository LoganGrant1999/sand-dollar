package com.sanddollar.service;

import com.sanddollar.budgeting.CategoryNormalizer;
import com.sanddollar.budgeting.DateWindows;
import com.sanddollar.budgeting.RefundHeuristics;
import com.sanddollar.budgeting.TransferHeuristics;
import com.sanddollar.config.PlaidDataSourceCondition;
import com.sanddollar.dto.aibudget.FinancialSnapshotResponse;
import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.User;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Conditional(PlaidDataSourceCondition.class)
@Transactional(readOnly = true)
public class PlaidSpendingDataProvider implements SpendingDataProvider {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Denver");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    private CategoryNormalizer categoryNormalizer;

    @Autowired
    private TransferHeuristics transferHeuristics;

    @Autowired
    private RefundHeuristics refundHeuristics;

    @Autowired
    private DateWindows dateWindows;

    public PlaidSpendingDataProvider(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public SnapshotDto getCurrentMonthSnapshot(Long userId, ZoneId zoneId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ZoneId effectiveZone = zoneId != null ? zoneId : DEFAULT_ZONE;
        LocalDate today = dateWindows.getCurrentDateInDenver();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endDate = today;
        String month = startOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(user, startOfMonth, endDate);

        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        BigDecimal income = BigDecimal.ZERO;

        for (Transaction txn : transactions) {
            if (Boolean.TRUE.equals(txn.getIsTransfer()) || isTransferOrRefund(txn)) {
                continue;
            }

            long amountCents = txn.getAmountCents() != null ? txn.getAmountCents() : 0L;
            BigDecimal amount = new BigDecimal(Math.abs(amountCents)).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

            if (amountCents > 0) {
                if (isIncome(txn)) {
                    income = income.add(amount);
                }
                continue;
            }

            String category = categoryNormalizer.normalizeCategory(txn.getCategoryTop(), txn.getCategorySub());
            categoryTotals.merge(category, amount, BigDecimal::add);
        }

        List<FinancialSnapshotResponse.CategoryActual> actuals = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            actuals.add(new FinancialSnapshotResponse.CategoryActual(entry.getKey(), entry.getValue()));
        }
        actuals.sort(Comparator.comparing(FinancialSnapshotResponse.CategoryActual::getCategory));

        BigDecimal totalExpenses = actuals.stream()
            .map(FinancialSnapshotResponse.CategoryActual::getActual)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netCashFlow = income.subtract(totalExpenses);
        BigDecimal savings = netCashFlow.max(BigDecimal.ZERO);

        FinancialSnapshotResponse.FinancialTotals totals = new FinancialSnapshotResponse.FinancialTotals(
            totalExpenses,
            savings,
            netCashFlow
        );

        return new SnapshotDto(month, income, actuals, totals);
    }

    private boolean isIncome(Transaction txn) {
        String categoryTop = txn.getCategoryTop();
        String categorySub = txn.getCategorySub();
        String name = txn.getName();

        return containsKeyword(categoryTop, "income")
            || containsKeyword(categoryTop, "payroll")
            || containsKeyword(categorySub, "income")
            || containsKeyword(categorySub, "payroll")
            || containsKeyword(name, "payroll")
            || containsKeyword(name, "deposit");
    }

    private boolean containsKeyword(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.US).contains(keyword.toLowerCase(Locale.US));
    }

    private boolean isTransferOrRefund(Transaction transaction) {
        // Use the new comprehensive transfer detection method
        boolean isTransfer = transferHeuristics.isTransfer(transaction);

        boolean isRefund = refundHeuristics.isRefund(
            transaction.getName(),
            transaction.getAmountCents()
        );

        return isTransfer || isRefund;
    }
}
