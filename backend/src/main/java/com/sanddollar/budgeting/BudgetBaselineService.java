package com.sanddollar.budgeting;

import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.User;
import com.sanddollar.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetBaselineService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryNormalizer categoryNormalizer;

    @Autowired
    private TransferHeuristics transferHeuristics;

    @Autowired
    private RefundHeuristics refundHeuristics;

    @Autowired
    private IncomeDetector incomeDetector;

    @Autowired
    private DateWindows dateWindows;

    @Autowired
    private Stats stats;

    public static class BudgetBaseline {
        private final long monthlyIncomeCents;
        private final Map<String, Long> monthlyExpensesByCategory;
        private final long totalMonthlyExpensesCents;
        private final PaycheckCadence paycheckCadence;
        private final Map<String, String> categoryConfidenceScores;

        public BudgetBaseline(long monthlyIncomeCents, Map<String, Long> monthlyExpensesByCategory,
                            long totalMonthlyExpensesCents, PaycheckCadence paycheckCadence,
                            Map<String, String> categoryConfidenceScores) {
            this.monthlyIncomeCents = monthlyIncomeCents;
            this.monthlyExpensesByCategory = monthlyExpensesByCategory;
            this.totalMonthlyExpensesCents = totalMonthlyExpensesCents;
            this.paycheckCadence = paycheckCadence;
            this.categoryConfidenceScores = categoryConfidenceScores;
        }

        public long getMonthlyIncomeCents() { return monthlyIncomeCents; }
        public Map<String, Long> getMonthlyExpensesByCategory() { return monthlyExpensesByCategory; }
        public long getTotalMonthlyExpensesCents() { return totalMonthlyExpensesCents; }
        public PaycheckCadence getPaycheckCadence() { return paycheckCadence; }
        public Map<String, String> getCategoryConfidenceScores() { return categoryConfidenceScores; }
    }

    public enum PaycheckCadence {
        WEEKLY, BIWEEKLY, SEMI_MONTHLY, MONTHLY, IRREGULAR
    }

    public BudgetBaseline calculateBaseline(User user) {
        DateWindows.DateRange last3Months = dateWindows.getLastNMonthsInDenver(3);
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(
            user, last3Months.getStart(), last3Months.getEnd());

        // Separate filtering for different types
        List<Transaction> incomeTransactions = filterIncomeTransactions(transactions);
        List<Transaction> expenseTransactions = filterExpenseTransactions(transactions);

        long monthlyIncome = calculateMonthlyIncome(incomeTransactions);
        Map<String, Long> monthlyExpenses = calculateMonthlyExpensesByCategory(expenseTransactions);
        long totalMonthlyExpenses = monthlyExpenses.values().stream().mapToLong(Long::longValue).sum();
        PaycheckCadence cadence = detectPaycheckCadence(incomeTransactions);
        Map<String, String> confidenceScores = calculateCategoryConfidenceScores(expenseTransactions);

        return new BudgetBaseline(monthlyIncome, monthlyExpenses, totalMonthlyExpenses,
                                cadence, confidenceScores);
    }

    private List<Transaction> filterIncomeTransactions(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> !t.getPending())
            .filter(incomeDetector::isBaselineIncome)
            .collect(Collectors.toList());
    }

    private List<Transaction> filterExpenseTransactions(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> !t.getPending())
            .filter(t -> t.getAmountCents() < 0)
            .filter(t -> !transferHeuristics.isTransfer(t))
            .filter(t -> !refundHeuristics.isRefund(t.getName(), t.getAmountCents()))
            .filter(t -> !isTransferCategory(t))
            .collect(Collectors.toList());
    }

    private boolean isTransferCategory(Transaction transaction) {
        String categoryTop = transaction.getCategoryTop();
        String categorySub = transaction.getCategorySub();

        return (categoryTop != null && categoryTop.toLowerCase().startsWith("transfer")) ||
               (categorySub != null &&
                (categorySub.equals("Credit Card Payment") ||
                 categorySub.equals("Transfer") ||
                 categorySub.equals("Transfer Out") ||
                 categorySub.equals("Transfer In")));
    }

    private long calculateMonthlyIncome(List<Transaction> incomeTransactions) {
        if (incomeTransactions.isEmpty()) {
            return 0L;
        }

        Map<LocalDate, Long> monthlyIncomes = groupByMonth(incomeTransactions)
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().mapToLong(Transaction::getAmountCents).sum()
            ));

        List<Double> monthlyValues = monthlyIncomes.values().stream()
            .map(Long::doubleValue)
            .collect(Collectors.toList());

        return Math.round(stats.winsorizedMean(monthlyValues, 10, 90));
    }

    private Map<String, Long> calculateMonthlyExpensesByCategory(List<Transaction> expenseTransactions) {
        Map<String, List<Transaction>> byCategory = expenseTransactions.stream()
            .collect(Collectors.groupingBy(this::normalizeTransactionCategory));

        Map<String, Long> monthlyExpenses = new HashMap<>();

        for (Map.Entry<String, List<Transaction>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<Transaction> categoryTransactions = entry.getValue();

            Map<LocalDate, Long> monthlyAmounts = groupByMonth(categoryTransactions)
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    monthEntry -> monthEntry.getValue().stream()
                        .mapToLong(t -> Math.abs(t.getAmountCents()))
                        .sum()
                ));

            List<Double> monthlyValues = monthlyAmounts.values().stream()
                .map(Long::doubleValue)
                .collect(Collectors.toList());

            long monthlyAverage = Math.round(stats.winsorizedMean(monthlyValues, 10, 90));
            monthlyExpenses.put(category, monthlyAverage);
        }

        return monthlyExpenses;
    }

    private String normalizeTransactionCategory(Transaction transaction) {
        return categoryNormalizer.normalizeCategory(
            transaction.getCategoryTop(),
            transaction.getCategorySub()
        );
    }

    private Map<LocalDate, List<Transaction>> groupByMonth(List<Transaction> transactions) {
        return transactions.stream()
            .collect(Collectors.groupingBy(t ->
                LocalDate.of(t.getDate().getYear(), t.getDate().getMonth(), 1)
            ));
    }

    private PaycheckCadence detectPaycheckCadence(List<Transaction> incomeTransactions) {
        List<Transaction> paycheckTransactions = incomeTransactions.stream()
            .sorted(Comparator.comparing(Transaction::getDate))
            .collect(Collectors.toList());

        if (paycheckTransactions.size() < 3) {
            return PaycheckCadence.IRREGULAR;
        }

        List<Integer> daysBetween = new ArrayList<>();
        for (int i = 1; i < paycheckTransactions.size(); i++) {
            LocalDate prev = paycheckTransactions.get(i - 1).getDate();
            LocalDate curr = paycheckTransactions.get(i).getDate();
            daysBetween.add((int) prev.until(curr).getDays());
        }

        double averageDays = stats.mean(daysBetween.stream().map(Integer::doubleValue).collect(Collectors.toList()));

        if (stats.isWithinTolerance(averageDays, 7, 20)) {
            return PaycheckCadence.WEEKLY;
        } else if (stats.isWithinTolerance(averageDays, 14, 20)) {
            return PaycheckCadence.BIWEEKLY;
        } else if (stats.isWithinTolerance(averageDays, 15, 20)) {
            return PaycheckCadence.SEMI_MONTHLY;
        } else if (stats.isWithinTolerance(averageDays, 30, 20)) {
            return PaycheckCadence.MONTHLY;
        } else {
            return PaycheckCadence.IRREGULAR;
        }
    }

    private Map<String, String> calculateCategoryConfidenceScores(List<Transaction> expenseTransactions) {
        Map<String, List<Transaction>> byCategory = expenseTransactions.stream()
            .collect(Collectors.groupingBy(this::normalizeTransactionCategory));

        Map<String, String> confidenceScores = new HashMap<>();

        for (Map.Entry<String, List<Transaction>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<Transaction> categoryTransactions = entry.getValue();

            Map<LocalDate, Long> monthlyAmounts = groupByMonth(categoryTransactions)
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    monthEntry -> monthEntry.getValue().stream()
                        .mapToLong(t -> Math.abs(t.getAmountCents()))
                        .sum()
                ));

            List<Double> monthlyValues = monthlyAmounts.values().stream()
                .map(Long::doubleValue)
                .collect(Collectors.toList());

            String confidence = stats.calculateConfidenceLevel(monthlyValues);
            confidenceScores.put(category, confidence);
        }

        return confidenceScores;
    }
}