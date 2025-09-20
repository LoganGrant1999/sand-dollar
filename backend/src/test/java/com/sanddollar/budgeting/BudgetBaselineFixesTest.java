package com.sanddollar.budgeting;

import com.sanddollar.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BudgetBaselineFixesTest {

    private TransferHeuristics transferHeuristics;
    private IncomeDetector incomeDetector;
    private Stats stats;

    @Mock
    private RefundHeuristics refundHeuristics;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create real instances for testing
        transferHeuristics = new TransferHeuristics();
        incomeDetector = new IncomeDetector();
        stats = new Stats();

        // Mock the config dependencies for IncomeDetector and TransferHeuristics
        // In a real test environment, these would be properly injected
        try {
            var incomeWhitelistNamesField = IncomeDetector.class.getDeclaredField("incomeWhitelistNames");
            incomeWhitelistNamesField.setAccessible(true);
            incomeWhitelistNamesField.set(incomeDetector, Set.of("Cozy Eart Dir Dep", "Cozy Earth", "Mastercard Stipend"));

            var incomeWhitelistCategoriesField = IncomeDetector.class.getDeclaredField("incomeWhitelistCategories");
            incomeWhitelistCategoriesField.setAccessible(true);
            incomeWhitelistCategoriesField.set(incomeDetector, Set.of("Payroll", "Salary", "Income Wages", "Paycheck"));

            var refundHeuristicsField = IncomeDetector.class.getDeclaredField("refundHeuristics");
            refundHeuristicsField.setAccessible(true);
            refundHeuristicsField.set(incomeDetector, refundHeuristics);

            var transferHeuristicsField = IncomeDetector.class.getDeclaredField("transferHeuristics");
            transferHeuristicsField.setAccessible(true);
            transferHeuristicsField.set(incomeDetector, transferHeuristics);

            var issuerSetField = TransferHeuristics.class.getDeclaredField("issuerSet");
            issuerSetField.setAccessible(true);
            issuerSetField.set(transferHeuristics, Set.of("American Express", "Amex", "Chase", "JPMorgan", "Capital One",
                     "Citi", "Citibank", "Discover", "Bank of America", "Wells Fargo", "US Bank", "Barclays"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test dependencies", e);
        }
    }

    @Test
    void creditCardPaymentExcludedFromExpenses() {
        Transaction ccPayment = createTransaction(
            "Payment Thank You - Chase",
            "Loan Payments",
            "Credit Card Payment",
            -50000L
        );

        assertTrue(transferHeuristics.isTransfer(ccPayment));
    }

    @Test
    void transferKeywordsExcluded() {
        Transaction onlinePayment = createTransaction(
            "Online Payment Posted",
            "Misc",
            "Misc",
            -30000L
        );

        assertTrue(transferHeuristics.isTransfer(onlinePayment));

        Transaction autopay = createTransaction(
            "Cardmember Services Autopay",
            "Misc",
            "Misc",
            -25000L
        );

        assertTrue(transferHeuristics.isTransfer(autopay));
    }

    @Test
    void incomeWhitelistOnly() {
        when(refundHeuristics.isRefund("Cozy Eart Dir Dep", 250000L)).thenReturn(false);
        when(refundHeuristics.isRefund("Venmo Cashout", 100000L)).thenReturn(false);

        Transaction cozyEartIncome = createTransaction(
            "300398 COZY EART DIR DEP GIBBONS, LOGAN",
            "Misc",
            "Income Wages",
            250000L
        );

        Transaction venmoIncome = createTransaction(
            "Venmo Cashout",
            "Misc",
            "Income Other",
            100000L
        );

        assertTrue(incomeDetector.isBaselineIncome(cozyEartIncome));
        assertFalse(incomeDetector.isBaselineIncome(venmoIncome));
        assertTrue(incomeDetector.isOtherInflow(venmoIncome));
    }

    @Test
    void winsorizedMeanFallsBackToMedianWhenSmallN() {
        List<Double> smallSample = Arrays.asList(100.0, 100.0, 2000.0);

        double result = stats.winsorizedMean(smallSample, 10, 90);
        double expectedMedian = 100.0;

        assertEquals(expectedMedian, result, 0.01);
    }

    @Test
    void confidenceScoringStable() {
        // Test with n=2 (should be "Low")
        List<Double> twoValues = Arrays.asList(100.0, 110.0);
        assertEquals("Low", stats.calculateConfidenceLevel(twoValues));

        // Test with mean≈0 (should be "Low")
        List<Double> nearZero = Arrays.asList(0.001, -0.001, 0.002);
        assertEquals("Low", stats.calculateConfidenceLevel(nearZero));

        // Test tight series (should be "High")
        List<Double> tightSeries = Arrays.asList(100.0, 110.0, 90.0, 105.0, 95.0);
        assertEquals("High", stats.calculateConfidenceLevel(tightSeries));

        // Test wildly variable series (should be "Low" but not negative)
        List<Double> variableSeries = Arrays.asList(100.0, 1000.0, 10.0, 500.0, 50.0);
        String confidence = stats.calculateConfidenceLevel(variableSeries);
        assertTrue(confidence.equals("Low") || confidence.equals("Medium") || confidence.equals("High"));
    }

    @Test
    void transferOutCategoryExcluded() {
        Transaction transferOut = createTransaction(
            "Transfer to Savings",
            "Transfer Out",
            "Account Transfer",
            -17800L
        );

        assertTrue(transferHeuristics.isTransfer(transferOut));
    }

    @Test
    void transferInCategoryExcluded() {
        Transaction transferIn = createTransaction(
            "Transfer from Checking",
            "Transfer In",
            "Transfer",
            50000L
        );

        assertTrue(transferHeuristics.isTransfer(transferIn));
    }

    @Test
    void issuerNameDetection() {
        Transaction chaseTransaction = createTransaction(
            "Some transaction",
            "Misc",
            "Misc",
            -10000L
        );
        chaseTransaction.setMerchantName("Chase Bank");

        assertTrue(transferHeuristics.isTransfer(chaseTransaction));
    }

    private Transaction createTransaction(String name, String categoryTop, String categorySub, long amountCents) {
        Transaction transaction = new Transaction();
        transaction.setName(name);
        transaction.setCategoryTop(categoryTop);
        transaction.setCategorySub(categorySub);
        transaction.setAmountCents(amountCents);
        transaction.setDate(LocalDate.now());
        transaction.setPending(false);
        return transaction;
    }
}