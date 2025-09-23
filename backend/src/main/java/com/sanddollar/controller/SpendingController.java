package com.sanddollar.controller;

import com.sanddollar.dto.CategorySpendResponse;
import com.sanddollar.dto.DailySpendResponse;
import com.sanddollar.entity.User;
import com.sanddollar.entity.Account;
import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.BalanceSnapshot;
import com.sanddollar.repository.AccountRepository;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.repository.BalanceSnapshotRepository;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.SpendingService;
import com.sanddollar.service.OpenAIService;
import com.sanddollar.budgeting.TransferHeuristics;
import com.sanddollar.budgeting.DateWindows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class SpendingController {

    @Autowired
    private SpendingService spendingService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;

    @Autowired
    private TransferHeuristics transferHeuristics;

    @Autowired
    private DateWindows dateWindows;

    @Autowired
    private OpenAIService openAIService;

    @GetMapping("/balances/total")
    public ResponseEntity<?> getTotalBalance(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            Map<String, Object> balance = spendingService.getTotalBalance(user);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get balance: " + e.getMessage()));
        }
    }

    @GetMapping("/spend/daily")
    public ResponseEntity<?> getDailySpending(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            DailySpendResponse response = spendingService.getDailySpending(user, days);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get daily spending: " + e.getMessage()));
        }
    }

    @GetMapping("/spend/categories")
    public ResponseEntity<?> getCategorySpending(
            @RequestParam(defaultValue = "30d") String range,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            CategorySpendResponse response = spendingService.getCategorySpending(user, range);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get category spending: " + e.getMessage()));
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            List<Account> accounts = accountRepository.findByUser(user);
            
            // Create a response with balance information
            List<Map<String, Object>> accountsWithBalance = new ArrayList<>();
            for (Account account : accounts) {
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("id", account.getId());

                // Improve display name for Venmo accounts
                String displayName = account.getName();
                if (account.getInstitutionName() != null &&
                    account.getInstitutionName().toLowerCase().contains("venmo") &&
                    "Personal Profile".equals(displayName)) {
                    displayName = "Venmo";
                }
                accountData.put("name", displayName);

                accountData.put("type", account.getType());
                accountData.put("subtype", account.getSubtype());
                accountData.put("mask", account.getMask());
                accountData.put("institutionName", account.getInstitutionName());
                
                // Get the latest balance for this account
                Optional<BalanceSnapshot> latestBalance = balanceSnapshotRepository.findTopByAccountOrderByAsOfDesc(account);
                if (latestBalance.isPresent()) {
                    // Send balance in cents as the full amount for frontend
                    long balance = latestBalance.get().getCurrentCents();
                    System.out.println("DEBUG: Account " + account.getName() + " balance in cents: " + balance);
                    accountData.put("balance", balance);
                } else {
                    accountData.put("balance", 0);
                }
                
                accountsWithBalance.add(accountData);
            }
            
            return ResponseEntity.ok(accountsWithBalance);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get accounts: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            List<Transaction> allTransactions = transactionRepository.findByAccountUserOrderByDateDesc(user);
            
            // Create response data without circular references
            List<Map<String, Object>> transactionData = allTransactions.stream()
                .limit(limit)
                .map(transaction -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", transaction.getId());
                    data.put("description", transaction.getName());
                    data.put("category", transaction.getCategoryTop() != null ? transaction.getCategoryTop() : "Other");
                    data.put("amount", transaction.getAmountCents()); // Send amount in cents
                    data.put("date", transaction.getDate().toString());
                    return data;
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(transactionData);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/balances/trend")
    public ResponseEntity<?> getBalanceTrend(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            System.out.println("Balance trend requested for user: " + user.getId() + " (" + user.getEmail() + ")");
            
            // Get balance snapshots from the last 'days' period
            java.time.Instant since = java.time.Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS);
            System.out.println("Looking for balance snapshots since: " + since);
            List<BalanceSnapshot> snapshots = balanceSnapshotRepository.findByUserSince(user, since);
            System.out.println("Found " + snapshots.size() + " balance snapshots");
            
            // Group by date and sum balances
            Map<String, Long> dailyBalances = new HashMap<>();
            for (BalanceSnapshot snapshot : snapshots) {
                String date = snapshot.getAsOf().atZone(java.time.ZoneOffset.UTC).toLocalDate().toString(); // YYYY-MM-DD
                dailyBalances.put(date, 
                    dailyBalances.getOrDefault(date, 0L) + snapshot.getCurrentCents());
                System.out.println("Processing snapshot with date: " + date + ", balance: " + (snapshot.getCurrentCents() / 100.0));
            }
            
            System.out.println("Grouped into " + dailyBalances.size() + " unique dates");
            
            // Create response data
            List<Map<String, Object>> trendData = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dailyBalances.entrySet()) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", entry.getKey());
                dataPoint.put("balance", entry.getValue()); // Send balance in cents
                trendData.add(dataPoint);
            }
            
            // Sort by date
            trendData.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));
            
            System.out.println("=== Final trend data has " + trendData.size() + " data points ===");
            if (!trendData.isEmpty()) {
                System.out.println("First data point: " + trendData.get(0));
                System.out.println("Last data point: " + trendData.get(trendData.size() - 1));
            }
            
            return ResponseEntity.ok(trendData);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get balance trend: " + e.getMessage()));
        }
    }

    @GetMapping("/spending/summary")
    public ResponseEntity<?> getSpendingSummary(
            @RequestParam(defaultValue = "30") int period,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();

            // Use Month-to-Date (MTD) instead of rolling 30 days
            LocalDate today = dateWindows.getCurrentDateInDenver();
            LocalDate endDate = today;
            LocalDate startDate = endDate.withDayOfMonth(1); // First day of current month

            List<Transaction> transactions = transactionRepository.findByUserAndDateRange(
                user, startDate, endDate);
            
            long totalIncome = 0;
            long totalExpenses = 0;
            Map<String, Long> categorySpending = new HashMap<>();
            
            for (Transaction transaction : transactions) {
                long amount = transaction.getAmountCents();

                // Skip credit card payments and transfers using comprehensive filtering
                if (transferHeuristics.isTransfer(transaction)) {
                    continue; // Skip this transaction
                }

                if (amount > 0) {
                    totalIncome += amount;
                } else {
                    totalExpenses += Math.abs(amount);
                    String category = transaction.getCategoryTop() != null ?
                        transaction.getCategoryTop() : "Other";
                    categorySpending.put(category,
                        categorySpending.getOrDefault(category, 0L) + Math.abs(amount));
                }
            }
            
            // Convert categorySpending to the format expected by frontend
            List<Map<String, Object>> categories = new ArrayList<>();
            for (Map.Entry<String, Long> entry : categorySpending.entrySet()) {
                Map<String, Object> category = new HashMap<>();
                category.put("category", entry.getKey());
                category.put("amount", entry.getValue());
                category.put("percentage", totalExpenses > 0 ? 
                    Math.round((double) entry.getValue() / totalExpenses * 100) : 0);
                categories.add(category);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalIncome", totalIncome);
            response.put("totalExpenses", totalExpenses);
            response.put("netCashFlow", totalIncome - totalExpenses); // Changed from netIncome to netCashFlow
            response.put("categories", categories); // Changed from categorySpending to categories
            response.put("period", period);
            response.put("transactionCount", transactions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get spending summary: " + e.getMessage()));
        }
    }

    @GetMapping("/spending/analytics")
    public ResponseEntity<?> getSpendingAnalytics(
            @RequestParam(defaultValue = "30") int period,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(period);
            
            List<Transaction> transactions = transactionRepository.findByUserAndDateRange(
                user, startDate, endDate);
            
            // Filter only expense transactions (negative amounts) and by category if specified
            List<Transaction> expenses = transactions.stream()
                .filter(t -> t.getAmountCents() < 0)
                .filter(t -> category == null || "all".equals(category) ||
                    (t.getCategoryTop() != null && t.getCategoryTop().equalsIgnoreCase(category)))
                .collect(Collectors.toList());
            
            long totalSpent = expenses.stream()
                .mapToLong(t -> Math.abs(t.getAmountCents()))
                .sum();
            
            double averageDaily = expenses.isEmpty() ? 0.0 : (double) totalSpent / period / 100.0;
            
            // Category breakdown
            Map<String, Long> categorySpending = new HashMap<>();
            Map<String, Integer> categoryCount = new HashMap<>();
            
            for (Transaction transaction : expenses) {
                String txnCategory = transaction.getCategoryTop() != null ?
                    transaction.getCategoryTop() : "Other";
                long amount = Math.abs(transaction.getAmountCents());
                categorySpending.put(txnCategory,
                    categorySpending.getOrDefault(txnCategory, 0L) + amount);
                categoryCount.put(txnCategory,
                    categoryCount.getOrDefault(txnCategory, 0) + 1);
            }
            
            List<Map<String, Object>> categories = new ArrayList<>();
            for (Map.Entry<String, Long> entry : categorySpending.entrySet()) {
                Map<String, Object> categoryData = new HashMap<>();
                categoryData.put("category", entry.getKey());
                categoryData.put("amount", entry.getValue() / 100.0); // Convert to dollars
                categoryData.put("count", categoryCount.get(entry.getKey()));
                categoryData.put("percentage", totalSpent > 0 ?
                    (double) entry.getValue() / totalSpent * 100 : 0);
                categories.add(categoryData);
            }
            
            // Daily trends (simplified - group by date)
            Map<String, Long> dailySpending = new HashMap<>();
            for (Transaction transaction : expenses) {
                String date = transaction.getDate().toString();
                long amount = Math.abs(transaction.getAmountCents());
                dailySpending.put(date, 
                    dailySpending.getOrDefault(date, 0L) + amount);
            }
            
            List<Map<String, Object>> trends = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dailySpending.entrySet()) {
                Map<String, Object> trend = new HashMap<>();
                trend.put("date", entry.getKey());
                trend.put("amount", entry.getValue() / 100.0); // Convert to dollars
                trends.add(trend);
            }
            
            // Sort trends by date
            trends.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));
            
            // Top merchants (group by transaction name/description)
            Map<String, Long> merchantSpending = new HashMap<>();
            Map<String, Integer> merchantCount = new HashMap<>();
            
            for (Transaction transaction : expenses) {
                String merchant = transaction.getName() != null ? 
                    transaction.getName() : "Unknown Merchant";
                long amount = Math.abs(transaction.getAmountCents());
                merchantSpending.put(merchant, 
                    merchantSpending.getOrDefault(merchant, 0L) + amount);
                merchantCount.put(merchant, 
                    merchantCount.getOrDefault(merchant, 0) + 1);
            }
            
            List<Map<String, Object>> topMerchants = new ArrayList<>();
            for (Map.Entry<String, Long> entry : merchantSpending.entrySet()) {
                Map<String, Object> merchant = new HashMap<>();
                merchant.put("merchant", entry.getKey());
                merchant.put("amount", entry.getValue() / 100.0); // Convert to dollars
                merchant.put("count", merchantCount.get(entry.getKey()));
                topMerchants.add(merchant);
            }
            
            // Sort by amount and take top 10
            topMerchants.sort((a, b) -> 
                Double.compare((Double) b.get("amount"), (Double) a.get("amount")));
            topMerchants = topMerchants.stream().limit(10).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalSpent", totalSpent / 100.0); // Convert to dollars
            response.put("averageDaily", averageDaily);
            response.put("categories", categories);
            response.put("trends", trends);
            response.put("topMerchants", topMerchants);
            response.put("period", period);
            response.put("transactionCount", expenses.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get spending analytics: " + e.getMessage()));
        }
    }

    @GetMapping("/spending/simplified-categories")
    public ResponseEntity<?> getSimplifiedCategorySpending(
            @RequestParam(defaultValue = "30") int period,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(period);

            List<Transaction> transactions = transactionRepository.findByUserAndDateRange(
                user, startDate, endDate);

            // Filter only expense transactions (negative amounts) and skip transfers
            List<Transaction> expenses = transactions.stream()
                .filter(t -> t.getAmountCents() < 0)
                .filter(t -> !transferHeuristics.isTransfer(t))
                .collect(Collectors.toList());

            long totalSpent = expenses.stream()
                .mapToLong(t -> Math.abs(t.getAmountCents()))
                .sum();

            // Group by original category
            Map<String, Long> originalCategorySpending = new HashMap<>();
            for (Transaction transaction : expenses) {
                String category = transaction.getCategoryTop() != null ?
                    transaction.getCategoryTop() : "Other";
                long amount = Math.abs(transaction.getAmountCents());
                originalCategorySpending.put(category,
                    originalCategorySpending.getOrDefault(category, 0L) + amount);
            }

            // Get simplified category names using OpenAI
            List<String> originalCategories = new ArrayList<>(originalCategorySpending.keySet());
            Map<String, String> categoryMapping = openAIService.simplifyCategoryNames(originalCategories);

            // Aggregate spending by simplified categories
            Map<String, Long> simplifiedCategorySpending = new HashMap<>();
            for (Map.Entry<String, Long> entry : originalCategorySpending.entrySet()) {
                String originalCategory = entry.getKey();
                String simplifiedCategory = categoryMapping.getOrDefault(originalCategory, originalCategory);
                long amount = entry.getValue();

                simplifiedCategorySpending.put(simplifiedCategory,
                    simplifiedCategorySpending.getOrDefault(simplifiedCategory, 0L) + amount);
            }

            // Create response list
            List<Map<String, Object>> categories = new ArrayList<>();
            for (Map.Entry<String, Long> entry : simplifiedCategorySpending.entrySet()) {
                Map<String, Object> category = new HashMap<>();
                category.put("category", entry.getKey());
                category.put("amount", entry.getValue() / 100.0); // Convert to dollars
                category.put("percentage", totalSpent > 0 ?
                    (double) entry.getValue() / totalSpent * 100 : 0);
                categories.add(category);
            }

            // Sort by amount descending
            categories.sort((a, b) ->
                Double.compare((Double) b.get("amount"), (Double) a.get("amount")));

            Map<String, Object> response = new HashMap<>();
            response.put("totalSpent", totalSpent / 100.0); // Convert to dollars
            response.put("categories", categories);
            response.put("period", period);
            response.put("transactionCount", expenses.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get simplified category spending: " + e.getMessage()));
        }
    }
}