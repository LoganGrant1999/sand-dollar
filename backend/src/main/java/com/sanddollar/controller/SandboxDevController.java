package com.sanddollar.controller;

import com.sanddollar.dto.*;
import com.sanddollar.entity.*;
import com.sanddollar.service.*;
import com.sanddollar.repository.*;
import com.sanddollar.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/dev/sandbox")
@Profile("dev")
public class SandboxDevController {

    private static final Logger logger = LoggerFactory.getLogger(SandboxDevController.class);

    @Autowired
    private PlaidItemRepository plaidItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;

    @Autowired
    private CryptoService cryptoService;

    @Value("${dev.auth-header}")
    private String devAuthHeader;

    private final Random random = new Random();

    private ResponseEntity<?> checkDevAuth(HttpServletRequest request) {
        String authHeader = request.getHeader("X-Dev-Auth");
        if (!devAuthHeader.equals(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Dev auth required", "hint", "Set X-Dev-Auth header"));
        }
        return null;
    }

    @PostMapping("/link-and-sync")
    public ResponseEntity<?> linkAndSync(@RequestBody(required = false) SandboxLinkRequest request,
                                        HttpServletRequest httpRequest,
                                        Authentication authentication) {
        ResponseEntity<?> authCheck = checkDevAuth(httpRequest);
        if (authCheck != null) return authCheck;

        if (request == null) {
            request = new SandboxLinkRequest();
        }

        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();

            logger.info("Creating sandbox item for user: {}", user.getEmail());

            // Step 1: Create a sandbox PlaidItem
            String itemId = "sandbox-item-" + UUID.randomUUID().toString();
            String accessToken = "sandbox-access-" + UUID.randomUUID().toString();
            String institutionName = getInstitutionName(request.getInstitutionId());

            String encryptedAccessToken = cryptoService.encrypt(accessToken);
            PlaidItem plaidItem = new PlaidItem(user, itemId, request.getInstitutionId(), 
                                              institutionName, encryptedAccessToken);
            plaidItem = plaidItemRepository.save(plaidItem);

            // Step 2: Create sandbox accounts
            int accountCount = createSandboxAccounts(plaidItem);

            // Step 3: Create sandbox transactions
            int txnsAdded = createSandboxTransactions(plaidItem, request.getStartDate(), request.getEndDate());

            // Step 4: Create sandbox balances
            long totalAvailableCents = createSandboxBalances(plaidItem);

            return ResponseEntity.ok(Map.of(
                "itemId", itemId,
                "accountCount", accountCount,
                "totalAvailableCents", totalAvailableCents,
                "txnsAdded", txnsAdded,
                "syncCursorShort", "sandbox_cursor_123...",
                "asOf", Instant.now().toString()
            ));

        } catch (Exception e) {
            logger.error("Error in link-and-sync", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to create sandbox item: " + e.getMessage()));
        }
    }

    @PostMapping("/mint-transactions")
    public ResponseEntity<?> mintTransactions(@RequestBody(required = false) SandboxMintRequest request,
                                            HttpServletRequest httpRequest,
                                            Authentication authentication) {
        ResponseEntity<?> authCheck = checkDevAuth(httpRequest);
        if (authCheck != null) return authCheck;

        if (request == null) {
            request = new SandboxMintRequest();
        }

        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();

            // Set default dates if not provided
            LocalDate startDate = request.getStartDate() != null 
                ? LocalDate.parse(request.getStartDate())
                : LocalDate.now().minusDays(7);
            LocalDate endDate = request.getEndDate() != null 
                ? LocalDate.parse(request.getEndDate())
                : LocalDate.now();

            List<Account> targetAccounts;
            if (request.getAccountId() != null) {
                Account account = accountRepository.findByAccountIdAndUser(request.getAccountId(), user);
                if (account == null) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Account not found: " + request.getAccountId()));
                }
                targetAccounts = List.of(account);
            } else {
                targetAccounts = accountRepository.findByUser(user);
            }

            int totalMinted = 0;

            for (Account account : targetAccounts) {
                totalMinted += mintTransactionsForAccount(account, request, startDate, endDate);
            }

            return ResponseEntity.ok(Map.of(
                "minted", totalMinted,
                "syncedAdded", totalMinted
            ));

        } catch (Exception e) {
            logger.error("Error in mint-transactions", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to mint transactions: " + e.getMessage()));
        }
    }

    @PostMapping("/fire-webhook")
    public ResponseEntity<?> fireWebhook(@RequestBody SandboxWebhookRequest request,
                                       HttpServletRequest httpRequest,
                                       Authentication authentication) {
        ResponseEntity<?> authCheck = checkDevAuth(httpRequest);
        if (authCheck != null) return authCheck;

        try {
            logger.info("Firing webhook for item: {} with code: {}", request.getItemId(), request.getWebhookCode());
            
            // In a real implementation, this would trigger actual webhook
            // For now, just log and return success
            return ResponseEntity.ok(Map.of("fired", true));

        } catch (Exception e) {
            logger.error("Error in fire-webhook", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fire webhook: " + e.getMessage()));
        }
    }

    private String getInstitutionName(String institutionId) {
        return switch (institutionId) {
            case "ins_109508" -> "First Platypus Bank";
            case "ins_109509" -> "Houndstooth Bank";
            case "ins_109510" -> "Tartan Bank";
            default -> "Sandbox Bank";
        };
    }

    private int createSandboxAccounts(PlaidItem plaidItem) {
        // Create 2-3 demo accounts
        List<String[]> accountConfigs = List.of(
            new String[]{"Plaid Checking", "depository", "checking", "0000"},
            new String[]{"Plaid Savings", "depository", "savings", "1111"},
            new String[]{"Plaid Credit Card", "credit", "credit_card", "3333"}
        );

        int count = 0;
        for (String[] config : accountConfigs) {
            Account account = new Account(
                plaidItem.getUser(),
                plaidItem,
                "sandbox-" + UUID.randomUUID().toString(),
                config[3],
                config[0],
                plaidItem.getInstitutionName(),
                config[1],
                config[2]
            );
            accountRepository.save(account);
            count++;
        }

        logger.info("Created {} sandbox accounts for item {}", count, plaidItem.getItemId());
        return count;
    }

    private int createSandboxTransactions(PlaidItem plaidItem, String startDateStr, String endDateStr) {
        LocalDate startDate = startDateStr != null 
            ? LocalDate.parse(startDateStr)
            : LocalDate.now().minusDays(30);
        LocalDate endDate = endDateStr != null 
            ? LocalDate.parse(endDateStr)
            : LocalDate.now();

        List<Account> accounts = accountRepository.findByUser(plaidItem.getUser());
        List<String> merchants = List.of(
            "Starbucks", "McDonald's", "Target", "Amazon", "Uber", "Shell Gas Station",
            "Kroger", "Walmart", "Netflix", "Spotify", "Apple Store", "Best Buy"
        );
        List<String> categories = List.of(
            "Food and Drink", "Shops", "Transportation", "Entertainment", 
            "Gas Stations", "Groceries", "Online Services"
        );

        int totalTransactions = 0;

        for (Account account : accounts) {
            int transactionCount = "credit".equals(account.getType()) ? 15 : 25;
            
            for (int i = 0; i < transactionCount; i++) {
                LocalDate txnDate = startDate.plusDays(
                    random.nextLong(ChronoUnit.DAYS.between(startDate, endDate) + 1)
                );
                
                String merchant = merchants.get(random.nextInt(merchants.size()));
                String category = categories.get(random.nextInt(categories.size()));
                
                // Generate amount (negative for spending, positive for income occasionally)
                long amountCents;
                if ("credit".equals(account.getType())) {
                    // Credit cards: mostly negative (spending)
                    amountCents = -random.nextLong(500, 15000); // $5-$150
                } else if (random.nextDouble() < 0.15) {
                    // 15% chance of income
                    amountCents = random.nextLong(5000, 500000); // $50-$5000
                } else {
                    // Regular spending
                    amountCents = -random.nextLong(300, 20000); // $3-$200
                }

                Transaction transaction = new Transaction(
                    account,
                    "sandbox-txn-" + UUID.randomUUID().toString(),
                    txnDate,
                    merchant,
                    merchant,
                    amountCents,
                    category,
                    null
                );
                
                // Simple transfer detection
                transaction.setIsTransfer(merchant.toLowerCase().contains("transfer") || 
                                        random.nextDouble() < 0.05);
                
                transactionRepository.save(transaction);
                totalTransactions++;
            }
        }

        logger.info("Created {} sandbox transactions for item {}", totalTransactions, plaidItem.getItemId());
        return totalTransactions;
    }

    private long createSandboxBalances(PlaidItem plaidItem) {
        List<Account> accounts = accountRepository.findByUser(plaidItem.getUser());
        long totalAvailable = 0;

        for (Account account : accounts) {
            BalanceSnapshot snapshot = new BalanceSnapshot();
            snapshot.setAccount(account);
            
            long availableCents;
            long currentCents;
            
            if ("credit".equals(account.getType())) {
                // Credit cards: negative balance (owed)
                currentCents = -random.nextLong(50000, 300000); // -$500 to -$3000 owed
                availableCents = random.nextLong(200000, 500000); // $2000-$5000 available credit
            } else {
                // Checking/savings: positive balance
                availableCents = random.nextLong(100000, 1500000); // $1000 to $15000
                currentCents = availableCents + random.nextLong(0, 50000); // Slightly higher
            }
            
            snapshot.setAvailableCents(availableCents);
            snapshot.setCurrentCents(currentCents);
            
            balanceSnapshotRepository.save(snapshot);
            
            if ("credit".equals(account.getType())) {
                totalAvailable += availableCents; // Credit limit available
            } else {
                totalAvailable += availableCents; // Cash available
            }
        }

        logger.info("Created sandbox balances for item {}, total available: ${}", 
                   plaidItem.getItemId(), totalAvailable / 100.0);
        return totalAvailable;
    }

    private int mintTransactionsForAccount(Account account, SandboxMintRequest request, 
                                         LocalDate startDate, LocalDate endDate) {
        for (int i = 0; i < request.getCount(); i++) {
            LocalDate txnDate = startDate.plusDays(
                random.nextLong(ChronoUnit.DAYS.between(startDate, endDate) + 1)
            );
            
            String merchant = request.getMerchantNames().get(
                random.nextInt(request.getMerchantNames().size()));
            String category = request.getCategory().get(
                random.nextInt(request.getCategory().size()));
            
            // Generate random amount in the specified range
            double amount = request.getAmountMin() + 
                (request.getAmountMax() - request.getAmountMin()) * random.nextDouble();
            long amountCents = -Math.round(amount * 100); // Negative for spending

            Transaction transaction = new Transaction(
                account,
                "minted-txn-" + UUID.randomUUID().toString(),
                txnDate,
                merchant,
                merchant,
                amountCents,
                category,
                null
            );
            
            transaction.setIsTransfer(false); // Minted transactions are not transfers
            transactionRepository.save(transaction);
        }

        logger.info("Minted {} transactions for account {}", request.getCount(), account.getAccountId());
        return request.getCount();
    }
}