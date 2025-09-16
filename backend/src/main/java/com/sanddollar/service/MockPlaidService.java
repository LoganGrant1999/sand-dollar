package com.sanddollar.service;

import com.sanddollar.dto.BudgetPrefillResponse;
import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.User;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MockPlaidService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockPlaidService.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get recent transactions for mock display
     */
    public List<Transaction> getRecentTransactions(Long userId, int sinceDays) {
        User user = userRepository.findById(userId).orElseThrow(() -> 
            new IllegalArgumentException("User not found: " + userId));
            
        LocalDate startDate = LocalDate.now().minusDays(sinceDays);
        LocalDate endDate = LocalDate.now();
        
        return transactionRepository.findByUserAndDateRange(user, startDate, endDate);
    }
    
    /**
     * Analyze transactions to prefill budget wizard
     */
    public BudgetPrefillResponse analyzeBudgetPrefill(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> 
            new IllegalArgumentException("User not found: " + userId));
            
        logger.info("Analyzing budget prefill for user: {}", userId);
        
        // Date ranges
        LocalDate now = LocalDate.now();
        LocalDate startDate90 = now.minusDays(90);
        LocalDate startDate30 = now.minusDays(30);
        
        List<Transaction> transactions90 = transactionRepository.findByUserAndDateRange(user, startDate90, now);
        List<Transaction> transactions30 = transactionRepository.findByUserAndDateRange(user, startDate30, now);
        
        // Estimate monthly income from payroll/salary deposits
        BigDecimal incomeEstimate = calculateIncomeEstimate(transactions30);
        
        // Detect recurring fixed expenses
        List<BudgetPrefillResponse.AllocationItem> fixedExpenses = detectFixedExpenses(transactions90);
        
        // Calculate variable spending suggestions
        List<BudgetPrefillResponse.AllocationItem> variableSuggestions = calculateVariableSuggestions(transactions90, fixedExpenses);
        
        return new BudgetPrefillResponse(incomeEstimate, fixedExpenses, variableSuggestions);
    }
    
    /**
     * Calculate income estimate from payroll/salary deposits over last 30 days
     */
    private BigDecimal calculateIncomeEstimate(List<Transaction> transactions30) {
        // Look for deposits (positive amounts) that might be payroll
        List<Transaction> possiblePayroll = transactions30.stream()
            .filter(t -> t.getAmountCents() > 0) // Deposits
            .filter(t -> !t.getIsTransfer()) // Not transfers
            .filter(t -> {
                String name = t.getName().toUpperCase();
                String category = t.getCategoryTop() != null ? t.getCategoryTop().toUpperCase() : "";
                // Look for payroll indicators
                return name.contains("PAYROLL") || name.contains("SALARY") || 
                       name.contains("DIRECT DEP") || name.contains("EMPLOYER") ||
                       category.contains("PAYROLL") || category.contains("SALARY") ||
                       category.contains("INCOME");
            })
            .collect(Collectors.toList());
        
        if (possiblePayroll.isEmpty()) {
            // Fallback: use largest deposits as potential income
            possiblePayroll = transactions30.stream()
                .filter(t -> t.getAmountCents() > 0)
                .filter(t -> !t.getIsTransfer())
                .filter(t -> t.getAmountCents() >= 50000) // At least $500
                .sorted((t1, t2) -> Long.compare(t2.getAmountCents(), t1.getAmountCents()))
                .limit(3) // Take top 3 largest deposits
                .collect(Collectors.toList());
        }
        
        if (possiblePayroll.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Sum and monthlyize
        long totalCents = possiblePayroll.stream()
            .mapToLong(Transaction::getAmountCents)
            .sum();
            
        BigDecimal total = BigDecimal.valueOf(totalCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        // If we have less than 30 days of data, extrapolate
        long daysOfData = Math.min(30, possiblePayroll.stream()
            .mapToLong(t -> java.time.temporal.ChronoUnit.DAYS.between(t.getDate(), LocalDate.now()))
            .max().orElse(30));
            
        if (daysOfData > 0 && daysOfData < 30) {
            total = total.multiply(BigDecimal.valueOf(30)).divide(BigDecimal.valueOf(daysOfData), 2, RoundingMode.HALF_UP);
        }
        
        return total;
    }
    
    /**
     * Detect recurring fixed expenses (rent, utilities, subscriptions, etc.)
     */
    private List<BudgetPrefillResponse.AllocationItem> detectFixedExpenses(List<Transaction> transactions90) {
        List<BudgetPrefillResponse.AllocationItem> fixedExpenses = new ArrayList<>();
        
        // Group transactions by merchant to detect recurring patterns
        Map<String, List<Transaction>> merchantGroups = transactions90.stream()
            .filter(t -> t.getAmountCents() < 0) // Only expenses
            .filter(t -> !t.getIsTransfer()) // Not transfers
            .filter(t -> t.getMerchantName() != null && !t.getMerchantName().isEmpty())
            .collect(Collectors.groupingBy(Transaction::getMerchantName));
            
        for (Map.Entry<String, List<Transaction>> entry : merchantGroups.entrySet()) {
            String merchant = entry.getKey();
            List<Transaction> merchantTxns = entry.getValue();
            
            // Only consider merchants with multiple transactions
            if (merchantTxns.size() >= 2) {
                // Check if transactions occur roughly monthly (within ±5 days)
                merchantTxns.sort(Comparator.comparing(Transaction::getDate));
                
                boolean isRecurring = true;
                List<Long> intervals = new ArrayList<>();
                
                for (int i = 1; i < merchantTxns.size(); i++) {
                    long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                        merchantTxns.get(i-1).getDate(), 
                        merchantTxns.get(i).getDate()
                    );
                    intervals.add(daysBetween);
                    
                    // Check if roughly monthly (25-35 days) or bi-weekly (12-16 days) or weekly (5-9 days)
                    if (!(daysBetween >= 25 && daysBetween <= 35) && 
                        !(daysBetween >= 12 && daysBetween <= 16) &&
                        !(daysBetween >= 5 && daysBetween <= 9)) {
                        isRecurring = false;
                        break;
                    }
                }
                
                if (isRecurring && !intervals.isEmpty()) {
                    // Calculate average amount
                    long totalCents = merchantTxns.stream()
                        .mapToLong(t -> Math.abs(t.getAmountCents()))
                        .sum();
                    BigDecimal avgAmount = BigDecimal.valueOf(totalCents)
                        .divide(BigDecimal.valueOf(merchantTxns.size()), 2, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    
                    // Convert to monthly if needed
                    double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(30.0);
                    if (avgInterval < 20) { // Weekly or bi-weekly, convert to monthly
                        avgAmount = avgAmount.multiply(BigDecimal.valueOf(30.0 / avgInterval))
                            .setScale(2, RoundingMode.HALF_UP);
                    }
                    
                    // Categorize based on merchant name or existing category
                    String category = categorizeFixedExpense(merchant, merchantTxns.get(0).getCategoryTop());
                    
                    fixedExpenses.add(new BudgetPrefillResponse.AllocationItem(category, avgAmount));
                }
            }
        }
        
        // Sort by amount descending and limit to top 10
        return fixedExpenses.stream()
            .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * Categorize fixed expenses based on merchant name and existing category
     */
    private String categorizeFixedExpense(String merchant, String existingCategory) {
        String merchantUpper = merchant.toUpperCase();
        String categoryUpper = existingCategory != null ? existingCategory.toUpperCase() : "";
        
        // Rent/Mortgage
        if (merchantUpper.contains("RENT") || merchantUpper.contains("MORTGAGE") || 
            merchantUpper.contains("PROPERTY") || categoryUpper.contains("RENT")) {
            return "Rent/Mortgage";
        }
        
        // Utilities
        if (merchantUpper.contains("ELECTRIC") || merchantUpper.contains("GAS") || 
            merchantUpper.contains("WATER") || merchantUpper.contains("UTILITY") ||
            categoryUpper.contains("UTILITIES")) {
            return "Utilities";
        }
        
        // Phone/Internet
        if (merchantUpper.contains("VERIZON") || merchantUpper.contains("ATT") || 
            merchantUpper.contains("T-MOBILE") || merchantUpper.contains("COMCAST") ||
            merchantUpper.contains("INTERNET") || merchantUpper.contains("PHONE")) {
            return "Phone/Internet";
        }
        
        // Insurance
        if (merchantUpper.contains("INSURANCE") || merchantUpper.contains("GEICO") || 
            merchantUpper.contains("STATE FARM") || categoryUpper.contains("INSURANCE")) {
            return "Insurance";
        }
        
        // Subscriptions
        if (merchantUpper.contains("NETFLIX") || merchantUpper.contains("SPOTIFY") || 
            merchantUpper.contains("SUBSCRIPTION") || merchantUpper.contains("ADOBE") ||
            merchantUpper.contains("AMAZON PRIME")) {
            return "Subscriptions";
        }
        
        // Default to merchant name or existing category
        return existingCategory != null && !existingCategory.isEmpty() ? existingCategory : merchant;
    }
    
    /**
     * Calculate variable spending suggestions based on category totals over 90 days
     */
    private List<BudgetPrefillResponse.AllocationItem> calculateVariableSuggestions(
            List<Transaction> transactions90, 
            List<BudgetPrefillResponse.AllocationItem> fixedExpenses) {
        
        // Get merchants that are already categorized as fixed to exclude them
        Set<String> fixedMerchants = transactions90.stream()
            .filter(t -> t.getAmountCents() < 0)
            .filter(t -> t.getMerchantName() != null)
            .collect(Collectors.groupingBy(Transaction::getMerchantName))
            .entrySet().stream()
            .filter(entry -> entry.getValue().size() >= 2) // Recurring
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        // Group remaining transactions by category
        Map<String, List<Transaction>> categoryGroups = transactions90.stream()
            .filter(t -> t.getAmountCents() < 0) // Only expenses
            .filter(t -> !t.getIsTransfer()) // Not transfers
            .filter(t -> t.getMerchantName() == null || !fixedMerchants.contains(t.getMerchantName())) // Not fixed
            .filter(t -> t.getCategoryTop() != null && !t.getCategoryTop().isEmpty())
            .collect(Collectors.groupingBy(Transaction::getCategoryTop));
        
        List<BudgetPrefillResponse.AllocationItem> variableSuggestions = new ArrayList<>();
        
        for (Map.Entry<String, List<Transaction>> entry : categoryGroups.entrySet()) {
            String category = entry.getKey();
            List<Transaction> categoryTxns = entry.getValue();
            
            // Calculate total spending in this category
            long totalCents = categoryTxns.stream()
                .mapToLong(t -> Math.abs(t.getAmountCents()))
                .sum();
                
            // Convert to monthly average (90 days = ~3 months)
            BigDecimal monthlyAvg = BigDecimal.valueOf(totalCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            
            // Apply smoothing and clamping for zero-based feasibility
            // Reduce by 10-20% to ensure it's achievable
            monthlyAvg = monthlyAvg.multiply(BigDecimal.valueOf(0.85))
                .setScale(2, RoundingMode.HALF_UP);
            
            if (monthlyAvg.compareTo(BigDecimal.valueOf(10)) >= 0) { // Only include categories >= $10/month
                variableSuggestions.add(new BudgetPrefillResponse.AllocationItem(category, monthlyAvg));
            }
        }
        
        // Sort by amount descending and limit to top 15
        return variableSuggestions.stream()
            .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
            .limit(15)
            .collect(Collectors.toList());
    }
}