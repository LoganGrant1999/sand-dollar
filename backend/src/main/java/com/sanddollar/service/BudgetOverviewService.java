package com.sanddollar.service;

import com.sanddollar.budgeting.BudgetBaselineService;
import com.sanddollar.dto.budget.BudgetOverviewDTO;
import com.sanddollar.dto.budget.CategoryRow;
import com.sanddollar.entity.User;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.impl.SpendingDataProviderFallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BudgetOverviewService {

    @Autowired
    @Qualifier("plaidSpendingDataProvider")
    private SpendingDataProvider plaidSpendingDataProvider;

    @Autowired
    private SpendingDataProviderFallback fallbackSpendingDataProvider;

    @Autowired
    private BudgetBaselineService budgetBaselineService;

    @Autowired
    private OpenAIService openAIService;

    public BudgetOverviewDTO getBudgetOverview() {
        User user = getCurrentUser();

        // Get MTD data from snapshot
        SpendingDataProvider activeProvider = plaidSpendingDataProvider != null ? plaidSpendingDataProvider : fallbackSpendingDataProvider;
        SpendingDataProvider.SnapshotDto snapshot = activeProvider.getCurrentMonthSnapshot(
            user.getId(),
            java.time.ZoneId.of("America/Denver")
        );

        // Get typical data from baseline
        BudgetBaselineService.BudgetBaseline baseline = budgetBaselineService.calculateBaseline(user);

        // Current month format
        String monthIso = snapshot.month() != null
            ? snapshot.month()
            : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // MTD values
        BigDecimal incomeMTD = snapshot.income() != null ? snapshot.income() : BigDecimal.ZERO;
        BigDecimal expensesMTD = snapshot.actualsByCategory() != null
            ? snapshot.actualsByCategory().stream()
                .map(category -> category.getActual() != null ? category.getActual() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
            : BigDecimal.ZERO;
        BigDecimal netMTD = incomeMTD.subtract(expensesMTD);
        double savingsRateMTD = incomeMTD.compareTo(BigDecimal.ZERO) != 0
            ? netMTD.divide(incomeMTD, 4, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        // Typical values
        BigDecimal incomeTypical = convertCentsToDollars(baseline.getMonthlyIncomeCents());
        BigDecimal expensesTypical = convertCentsToDollars(baseline.getTotalMonthlyExpensesCents());
        BigDecimal netTypical = incomeTypical.subtract(expensesTypical);
        double savingsRateTypical = incomeTypical.compareTo(BigDecimal.ZERO) != 0
            ? netTypical.divide(incomeTypical, 4, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        // Build category rows combining MTD and typical data
        List<CategoryRow> categoriesMTD = buildCategoryRows(snapshot, baseline);

        return new BudgetOverviewDTO(
            monthIso,
            incomeMTD,
            expensesMTD,
            netMTD,
            savingsRateMTD,
            incomeTypical,
            expensesTypical,
            netTypical,
            savingsRateTypical,
            categoriesMTD
        );
    }

    private List<CategoryRow> buildCategoryRows(SpendingDataProvider.SnapshotDto snapshot,
                                              BudgetBaselineService.BudgetBaseline baseline) {
        List<CategoryRow> rows = new ArrayList<>();

        // Get MTD categories
        Map<String, BigDecimal> mtdByCategory = snapshot.actualsByCategory() != null
            ? snapshot.actualsByCategory().stream()
                .collect(Collectors.toMap(
                    category -> category.getCategory(),
                    category -> category.getActual() != null ? category.getActual() : BigDecimal.ZERO,
                    (existing, replacement) -> replacement
                ))
            : Map.of();

        // Get typical categories with confidence scores
        Map<String, Long> typicalByCategory = baseline.getMonthlyExpensesByCategory();
        Map<String, String> confidenceScores = baseline.getCategoryConfidenceScores();

        // Combine all unique categories
        var allCategories = new java.util.HashSet<String>();
        allCategories.addAll(mtdByCategory.keySet());
        allCategories.addAll(typicalByCategory.keySet());

        // Get simplified category names
        Map<String, String> categoryMapping = openAIService.simplifyCategoryNames(new ArrayList<>(allCategories));

        // Build simplified category aggregations
        Map<String, BigDecimal> simplifiedMtdByCategory = new HashMap<>();
        Map<String, BigDecimal> simplifiedTypicalByCategory = new HashMap<>();
        Map<String, String> simplifiedConfidenceScores = new HashMap<>();

        for (String originalCategory : allCategories) {
            String simplifiedCategory = categoryMapping.getOrDefault(originalCategory, originalCategory);

            BigDecimal mtdAmount = mtdByCategory.getOrDefault(originalCategory, BigDecimal.ZERO);
            BigDecimal typicalAmount = convertCentsToDollars(typicalByCategory.getOrDefault(originalCategory, 0L));
            String confidence = confidenceScores.getOrDefault(originalCategory, "Low");

            // Aggregate amounts for simplified categories
            simplifiedMtdByCategory.put(simplifiedCategory,
                simplifiedMtdByCategory.getOrDefault(simplifiedCategory, BigDecimal.ZERO).add(mtdAmount));
            simplifiedTypicalByCategory.put(simplifiedCategory,
                simplifiedTypicalByCategory.getOrDefault(simplifiedCategory, BigDecimal.ZERO).add(typicalAmount));

            // Use highest confidence score for simplified category
            String existingConfidence = simplifiedConfidenceScores.get(simplifiedCategory);
            if (existingConfidence == null || "High".equals(confidence) ||
                ("Medium".equals(confidence) && "Low".equals(existingConfidence))) {
                simplifiedConfidenceScores.put(simplifiedCategory, confidence);
            }
        }

        // Create rows from simplified categories
        for (String simplifiedCategory : simplifiedMtdByCategory.keySet()) {
            BigDecimal mtdAmount = simplifiedMtdByCategory.get(simplifiedCategory);
            BigDecimal typicalAmount = simplifiedTypicalByCategory.get(simplifiedCategory);
            String confidence = simplifiedConfidenceScores.get(simplifiedCategory);

            rows.add(new CategoryRow(simplifiedCategory, mtdAmount, typicalAmount, confidence));
        }

        // Sort by MTD spending (descending)
        rows.sort((a, b) -> b.amountMTD().compareTo(a.amountMTD()));

        return rows;
    }

    private BigDecimal convertCentsToDollars(long cents) {
        return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getUser();
        }
        throw new RuntimeException("User not authenticated");
    }
}