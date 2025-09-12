package com.sanddollar.controller;

import com.sanddollar.entity.User;
import com.sanddollar.entity.Account;
import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.BalanceSnapshot;
import com.sanddollar.repository.*;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.MockBankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Development-only controller for mock data management
 * Only available when mock profile is active
 */
@RestController
@RequestMapping("/dev/mock")
@Profile("mock")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class MockDevController {
    private static final Logger logger = LoggerFactory.getLogger(MockDevController.class);

    @Autowired
    private MockBankService mockBankService;

    @Autowired
    private PlaidItemRepository plaidItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;

    /**
     * Seed mock data for the authenticated user
     * Creates mock accounts, balances, and 90 days of transactions
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedMockData(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "false") boolean force) {
        
        try {
            User user = userPrincipal.getUser();
            logger.info("Seeding mock data for user: {} (force: {})", user.getEmail(), force);

            // Check if data already exists
            List<Account> existingAccounts = accountRepository.findByUser(user);
            if (!existingAccounts.isEmpty() && !force) {
                return ResponseEntity.ok(Map.of(
                    "message", "Mock data already exists. Use force=true to regenerate.",
                    "accounts", existingAccounts.size(),
                    "transactions", transactionRepository.findByAccountUserOrderByDateDesc(user).size()
                ));
            }

            // If force=true, clean existing data first
            if (force) {
                cleanUserData(user);
            }

            // Create mock item and accounts
            String publicToken = "mock-public-token-" + user.getId();
            mockBankService.exchangePublicToken(user, publicToken);

            // Generate transactions
            mockBankService.generateMockTransactions(user);

            // Get summary data
            List<Account> accounts = accountRepository.findByUser(user);
            List<Transaction> transactions = transactionRepository.findByAccountUserOrderByDateDesc(user);
            List<BalanceSnapshot> balances = balanceSnapshotRepository.findRecentByUser(user);
            
            Long totalAvailableCents = balances.stream()
                .mapToLong(b -> b.getAvailableCents() != null ? b.getAvailableCents() : 0L)
                .sum();

            // Calculate date range
            LocalDate fromDate = transactions.isEmpty() ? LocalDate.now() : 
                transactions.stream().map(Transaction::getDate).min(LocalDate::compareTo).orElse(LocalDate.now());
            LocalDate toDate = transactions.isEmpty() ? LocalDate.now() : 
                transactions.stream().map(Transaction::getDate).max(LocalDate::compareTo).orElse(LocalDate.now());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mock data seeded successfully");
            response.put("accounts", accounts.size());
            response.put("transactions", transactions.size());
            response.put("totalAvailableCents", totalAvailableCents);
            response.put("from", fromDate.toString());
            response.put("to", toDate.toString());
            response.put("accountDetails", accounts.stream().map(a -> Map.of(
                "name", a.getName(),
                "type", a.getType(),
                "subtype", a.getSubtype()
            )).toList());

            logger.info("Successfully seeded mock data: {} accounts, {} transactions", 
                       accounts.size(), transactions.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error seeding mock data", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to seed mock data: " + e.getMessage()));
        }
    }

    /**
     * Reset mock data for the authenticated user
     * Deletes existing mock data and creates fresh data
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetMockData(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            User user = userPrincipal.getUser();
            logger.info("Resetting mock data for user: {}", user.getEmail());

            // Clean existing data
            cleanUserData(user);

            // Seed fresh data
            return seedMockData(userPrincipal, false);

        } catch (Exception e) {
            logger.error("Error resetting mock data", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to reset mock data: " + e.getMessage()));
        }
    }

    /**
     * Get mock data summary for the authenticated user
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMockDataSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            User user = userPrincipal.getUser();
            
            List<Account> accounts = accountRepository.findByUser(user);
            List<Transaction> transactions = transactionRepository.findByAccountUserOrderByDateDesc(user);
            List<BalanceSnapshot> balances = balanceSnapshotRepository.findRecentByUser(user);
            
            Long totalAvailableCents = balances.stream()
                .mapToLong(b -> b.getAvailableCents() != null ? b.getAvailableCents() : 0L)
                .sum();

            Map<String, Object> response = new HashMap<>();
            response.put("accounts", accounts.size());
            response.put("transactions", transactions.size());
            response.put("totalAvailableCents", totalAvailableCents);
            response.put("hasData", !accounts.isEmpty());
            response.put("userId", user.getId());
            response.put("userEmail", user.getEmail());

            if (!transactions.isEmpty()) {
                LocalDate fromDate = transactions.stream().map(Transaction::getDate).min(LocalDate::compareTo).orElse(LocalDate.now());
                LocalDate toDate = transactions.stream().map(Transaction::getDate).max(LocalDate::compareTo).orElse(LocalDate.now());
                response.put("from", fromDate.toString());
                response.put("to", toDate.toString());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting mock data summary", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get summary: " + e.getMessage()));
        }
    }

    /**
     * Clean all mock data for a user
     */
    private void cleanUserData(User user) {
        logger.info("Cleaning existing mock data for user: {}", user.getEmail());
        
        // Delete in order to avoid foreign key constraints
        List<Account> accounts = accountRepository.findByUser(user);
        for (Account account : accounts) {
            transactionRepository.deleteByAccount(account);
            balanceSnapshotRepository.deleteByAccount(account);
        }
        
        accountRepository.deleteByUser(user);
        plaidItemRepository.deleteByUser(user);
        
        logger.info("Cleaned mock data for user: {}", user.getEmail());
    }
}