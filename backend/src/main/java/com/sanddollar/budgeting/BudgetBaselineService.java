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
    private DateWindows dateWindows;

    @Autowired
    private Stats stats;

    public static class BudgetBaseline {
        private final long monthlyIncomeCents;
        private final Map<String, Long> monthlyExpensesByCategory;
        private final long totalMonthlyExpensesCents;
        private final PaycheckCadence paycheckCadence;
        private final Map<String, Double> categoryConfidenceScores;

        public BudgetBaseline(long monthlyIncomeCents, Map<String, Long> monthlyExpensesByCategory,
                            long totalMonthlyExpensesCents, PaycheckCadence paycheckCadence,
                            Map<String, Double> categoryConfidenceScores) {
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
        public Map<String, Double> getCategoryConfidenceScores() { return categoryConfidenceScores; }
    }

    public enum PaycheckCadence {
        WEEKLY, BIWEEKLY, SEMI_MONTHLY, MONTHLY, IRREGULAR
    }

    public BudgetBaseline calculateBaseline(User user) {
        DateWindows.DateRange last3Months = dateWindows.getLastNMonthsInDenver(3);
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(
            user, last3Months.getStart(), last3Months.getEnd());

        List<Transaction> cleanTransactions = filterTransactions(transactions);

        long monthlyIncome = calculateMonthlyIncome(cleanTransactions);
        Map<String, Long> monthlyExpenses = calculateMonthlyExpensesByCategory(cleanTransactions);
        long totalMonthlyExpenses = monthlyExpenses.values().stream().mapToLong(Long::longValue).sum();
        PaycheckCadence cadence = detectPaycheckCadence(cleanTransactions);
        Map<String, Double> confidenceScores = calculateCategoryConfidenceScores(cleanTransactions);

        return new BudgetBaseline(monthlyIncome, monthlyExpenses, totalMonthlyExpenses,
                                cadence, confidenceScores);
    }

    private List<Transaction> filterTransactions(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> !t.getPending())
            .filter(t -> !isTransferOrRefund(t))
            .collect(Collectors.toList());
    }

    private boolean isTransferOrRefund(Transaction transaction) {
        boolean isTransfer = transferHeuristics.isTransfer(
            transaction.getName(),
            transaction.getCategoryTop(),
            transaction.getCategorySub()
        );

        boolean isRefund = refundHeuristics.isRefund(
            transaction.getName(),
            transaction.getAmountCents()
        );

        return isTransfer || isRefund;
    }

    private long calculateMonthlyIncome(List<Transaction> transactions) {
        List<Transaction> incomeTransactions = transactions.stream()
            .filter(t -> t.getAmountCents() > 0)
            .collect(Collectors.toList());

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

    private Map<String, Long> calculateMonthlyExpensesByCategory(List<Transaction> transactions) {
        List<Transaction> expenseTransactions = transactions.stream()
            .filter(t -> t.getAmountCents() < 0)
            .collect(Collectors.toList());

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

    private PaycheckCadence detectPaycheckCadence(List<Transaction> transactions) {
        List<Transaction> incomeTransactions = transactions.stream()
            .filter(t -> t.getAmountCents() > 0)
            .filter(this::isLikelyPaycheck)
            .sorted(Comparator.comparing(Transaction::getDate))
            .collect(Collectors.toList());

        if (incomeTransactions.size() < 3) {
            return PaycheckCadence.IRREGULAR;
        }

        List<Integer> daysBetween = new ArrayList<>();
        for (int i = 1; i < incomeTransactions.size(); i++) {
            LocalDate prev = incomeTransactions.get(i - 1).getDate();
            LocalDate curr = incomeTransactions.get(i).getDate();
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

    private boolean isLikelyPaycheck(Transaction transaction) {
        if (transaction.getAmountCents() <= 0) {
            return false;
        }

        String name = transaction.getName().toLowerCase();
        String category = transaction.getCategoryTop() != null ?
            transaction.getCategoryTop().toLowerCase() : "";

        return name.contains("payroll") ||
               name.contains("salary") ||
               name.contains("wages") ||
               name.contains("direct deposit") ||
               category.contains("payroll") ||
               category.contains("salary");
    }

    private Map<String, Double> calculateCategoryConfidenceScores(List<Transaction> transactions) {
        Map<String, List<Transaction>> byCategory = transactions.stream()
            .filter(t -> t.getAmountCents() < 0)
            .collect(Collectors.groupingBy(this::normalizeTransactionCategory));

        Map<String, Double> confidenceScores = new HashMap<>();

        for (Map.Entry<String, List<Transaction>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<Transaction> categoryTransactions = entry.getValue();

            if (categoryTransactions.size() < 3) {
                confidenceScores.put(category, 0.3);
                continue;
            }

            Map<LocalDate, Long> monthlyAmounts = groupByMonth(categoryTransactions)
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    monthEntry -> monthEntry.getValue().stream()
                        .mapToLong(t -> Math.abs(t.getAmountCents()))
                        .sum()
                ));

            if (monthlyAmounts.size() < 2) {
                confidenceScores.put(category, 0.5);
                continue;
            }

            List<Double> monthlyValues = monthlyAmounts.values().stream()
                .map(Long::doubleValue)
                .collect(Collectors.toList());

            double mean = stats.mean(monthlyValues);
            double stdDev = stats.standardDeviation(monthlyValues);
            double coefficientOfVariation = mean > 0 ? stdDev / mean : 1.0;

            double confidence = Math.max(0.1, 1.0 - Math.min(1.0, coefficientOfVariation));
            confidenceScores.put(category, confidence);
        }

        return confidenceScores;
    }
}