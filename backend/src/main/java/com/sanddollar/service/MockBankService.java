package com.sanddollar.service;

import com.sanddollar.entity.*;
import com.sanddollar.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

@Service
@Profile("mock")
@Transactional
public class MockBankService implements BankDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(MockBankService.class);

    @Autowired
    private PlaidItemRepository plaidItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Mock data constants
    private static final String INSTITUTION_NAME = "MockBank";
    private static final List<String> ACCOUNT_CONFIGS = List.of(
        "MockBank Checking|1234|depository|checking",
        "MockBank Savings|5678|depository|savings", 
        "MockBank Credit Card|9012|credit|credit_card"
    );

    // Merchant data for realistic transactions
    private static final Map<String, TransactionTemplate> MERCHANTS = Map.ofEntries(
        // Food & Drink (25%)
        Map.entry("STARBUCKS", new TransactionTemplate("Food and Drink", "Coffee", 400, 800)),
        Map.entry("CHIPOTLE", new TransactionTemplate("Food and Drink", "Fast Food", 800, 1500)),
        Map.entry("DOORDASH", new TransactionTemplate("Food and Drink", "Food Delivery", 1200, 3500)),
        Map.entry("UBER EATS", new TransactionTemplate("Food and Drink", "Food Delivery", 1000, 3000)),
        Map.entry("LOCAL CAFE", new TransactionTemplate("Food and Drink", "Coffee", 300, 700)),
        Map.entry("PIZZA HUT", new TransactionTemplate("Food and Drink", "Fast Food", 1500, 2500)),
        Map.entry("SUBWAY", new TransactionTemplate("Food and Drink", "Fast Food", 600, 1200)),
        
        // Groceries (20%)
        Map.entry("COSTCO", new TransactionTemplate("Groceries", "Warehouse Store", 5000, 12000)),
        Map.entry("TARGET", new TransactionTemplate("Groceries", "Supermarket", 3000, 8000)),
        Map.entry("WALMART", new TransactionTemplate("Groceries", "Supermarket", 2000, 7000)),
        Map.entry("WHOLE FOODS", new TransactionTemplate("Groceries", "Supermarket", 2500, 6000)),
        Map.entry("TRADER JOES", new TransactionTemplate("Groceries", "Supermarket", 2000, 5000)),
        Map.entry("SAFEWAY", new TransactionTemplate("Groceries", "Supermarket", 2000, 6000)),
        
        // Transport (10%)
        Map.entry("UBER", new TransactionTemplate("Transportation", "Rideshare", 800, 3000)),
        Map.entry("LYFT", new TransactionTemplate("Transportation", "Rideshare", 800, 3000)),
        Map.entry("SHELL", new TransactionTemplate("Transportation", "Gas Station", 3000, 6000)),
        Map.entry("BP", new TransactionTemplate("Transportation", "Gas Station", 2500, 5500)),
        Map.entry("GAS & GO", new TransactionTemplate("Transportation", "Gas Station", 2000, 5000)),
        Map.entry("PARKING METER", new TransactionTemplate("Transportation", "Parking", 200, 500)),
        
        // Shops (15%)
        Map.entry("AMAZON", new TransactionTemplate("Shops", "Online", 1500, 15000)),
        Map.entry("APPLE STORE", new TransactionTemplate("Shops", "Electronics", 5000, 50000)),
        Map.entry("BEST BUY", new TransactionTemplate("Shops", "Electronics", 3000, 25000)),
        Map.entry("HOME DEPOT", new TransactionTemplate("Shops", "Home Improvement", 2000, 15000)),
        Map.entry("CVS", new TransactionTemplate("Shops", "Pharmacy", 500, 2000)),
        
        // Bills (10%)
        Map.entry("NETFLIX", new TransactionTemplate("Entertainment", "Streaming", 1599, 1599)),
        Map.entry("SPOTIFY", new TransactionTemplate("Entertainment", "Streaming", 999, 999)),
        Map.entry("APPLE MUSIC", new TransactionTemplate("Entertainment", "Streaming", 999, 999)),
        Map.entry("COMCAST XFINITY", new TransactionTemplate("Bills", "Internet", 8000, 12000)),
        Map.entry("PG&E", new TransactionTemplate("Bills", "Utilities", 6000, 18000)),
        Map.entry("AT&T", new TransactionTemplate("Bills", "Phone", 4000, 9000)),
        
        // Entertainment (8%)
        Map.entry("AMC THEATERS", new TransactionTemplate("Entertainment", "Movies", 1200, 2500)),
        Map.entry("SPOTIFY PREMIUM", new TransactionTemplate("Entertainment", "Music", 999, 999)),
        Map.entry("BOWLING ALLEY", new TransactionTemplate("Entertainment", "Recreation", 2000, 4000)),
        Map.entry("CONCERT VENUE", new TransactionTemplate("Entertainment", "Concerts", 5000, 15000)),
        
        // Health (5%)
        Map.entry("CVS PHARMACY", new TransactionTemplate("Healthcare", "Pharmacy", 1000, 5000)),
        Map.entry("WALGREENS", new TransactionTemplate("Healthcare", "Pharmacy", 800, 4000)),
        Map.entry("DENTIST OFFICE", new TransactionTemplate("Healthcare", "Dental", 15000, 50000)),
        Map.entry("DOCTOR VISIT", new TransactionTemplate("Healthcare", "Medical", 20000, 40000))
    );

    private static final List<String> INCOME_SOURCES = List.of(
        "PAYROLL *SANDBOX CORP",
        "PAYROLL *TECH SOLUTIONS LLC",
        "PAYROLL *STARTUP COMPANY"
    );

    @Override
    public String createLinkToken(User user) {
        logger.info("Creating mock link token for user: {}", user.getEmail());
        return "link-mock-token-" + UUID.randomUUID().toString();
    }

    @Override
    public PlaidItem exchangePublicToken(User user, String publicToken) {
        logger.info("Exchanging mock public token for user: {}", user.getEmail());
        
        // Check if user already has a mock item
        List<PlaidItem> existingItems = plaidItemRepository.findByUser(user);
        if (!existingItems.isEmpty()) {
            return existingItems.get(0);
        }

        PlaidItem plaidItem = new PlaidItem();
        plaidItem.setUser(user);
        plaidItem.setItemId("mock-item-" + user.getId());
        plaidItem.setAccessTokenEncrypted("mock-token-encrypted-" + UUID.randomUUID().toString());
        plaidItem.setInstitutionName(INSTITUTION_NAME);
        plaidItem.setCreatedAt(Instant.now());
        
        PlaidItem savedItem = plaidItemRepository.save(plaidItem);
        
        // Create accounts and generate demo data
        createMockAccounts(savedItem);
        generateMockTransactions(user);
        
        return savedItem;
    }

    @Override
    public Map<String, Object> syncTransactions(User user) {
        logger.info("Syncing mock transactions for user: {}", user.getEmail());
        
        // If no transactions exist, generate them
        List<Transaction> existingTransactions = transactionRepository.findByAccountUserOrderByDateDesc(user);
        if (existingTransactions.isEmpty()) {
            generateMockTransactions(user);
            existingTransactions = transactionRepository.findByAccountUserOrderByDateDesc(user);
        }
        
        return Map.of(
            "success", true,
            "transactionCount", existingTransactions.size(),
            "message", "Mock transactions synced"
        );
    }

    @Override
    public Map<String, Object> fetchBalances(User user) {
        logger.info("Fetching mock balances for user: {}", user.getEmail());
        
        // Refresh balance snapshots
        List<Account> accounts = accountRepository.findByUser(user);
        for (Account account : accounts) {
            updateAccountBalance(account);
        }
        
        // Calculate totals
        List<BalanceSnapshot> recentBalances = balanceSnapshotRepository.findRecentByUser(user);
        Long totalAvailable = recentBalances.stream()
            .mapToLong(b -> b.getAvailableCents() != null ? b.getAvailableCents() : 0L)
            .sum();
        
        return Map.of(
            "totalAvailableCents", totalAvailable,
            "asOf", Instant.now(),
            "accountCount", accounts.size()
        );
    }

    @Override
    public void handleWebhook(String itemId, String webhookType, String webhookCode) {
        logger.info("Received mock webhook - Item: {}, Type: {}, Code: {}", itemId, webhookType, webhookCode);
        // Mock webhook handling - no action needed
    }

    /**
     * Create mock accounts for the user
     */
    private void createMockAccounts(PlaidItem plaidItem) {
        for (String config : ACCOUNT_CONFIGS) {
            String[] parts = config.split("\\|");
            String name = parts[0];
            String mask = parts[1];
            String type = parts[2];
            String subtype = parts[3];

            Account account = new Account();
            account.setUser(plaidItem.getUser());
            account.setPlaidItem(plaidItem);
            account.setAccountId("mock-account-" + mask + "-" + plaidItem.getUser().getId());
            account.setName(name);
            account.setInstitutionName(INSTITUTION_NAME);
            account.setType(type);
            account.setSubtype(subtype);
            account.setCreatedAt(Instant.now());
            
            Account savedAccount = accountRepository.save(account);
            createInitialBalance(savedAccount, type);
        }
    }

    /**
     * Create initial balance for account
     */
    private void createInitialBalance(Account account, String accountType) {
        Random random = new Random(account.getUser().getId() + account.getName().hashCode());
        
        Long availableBalance;
        switch (accountType) {
            case "depository":
                if (account.getSubtype().equals("checking")) {
                    availableBalance = 200_00L + random.nextLong(8000_00L); // $200 - $8000
                } else { // savings
                    availableBalance = 300_00L + random.nextLong(15000_00L); // $300 - $15000
                }
                break;
            case "credit":
                availableBalance = -(200_00L + random.nextLong(1800_00L)); // -$200 to -$2000
                break;
            default:
                availableBalance = 0L;
        }
        
        BalanceSnapshot snapshot = new BalanceSnapshot();
        snapshot.setAccount(account);
        snapshot.setAvailableCents(availableBalance);
        snapshot.setCurrentCents(availableBalance);
        // asOf timestamp will be set automatically by @CreationTimestamp
        
        balanceSnapshotRepository.save(snapshot);
    }

    /**
     * Update account balance based on transactions
     */
    private void updateAccountBalance(Account account) {
        // Calculate balance from transactions
        List<Transaction> transactions = transactionRepository.findByAccount(account);
        Long totalCents = transactions.stream()
            .mapToLong(t -> t.getAmountCents())
            .sum();
        
        // Get initial balance or use default
        List<BalanceSnapshot> existing = balanceSnapshotRepository.findByAccountOrderByAsOfDesc(account);
        Long initialBalance = 0L;
        if (!existing.isEmpty()) {
            initialBalance = existing.get(0).getAvailableCents();
        }
        
        Long currentBalance = initialBalance + totalCents;
        
        BalanceSnapshot snapshot = new BalanceSnapshot();
        snapshot.setAccount(account);
        snapshot.setAvailableCents(currentBalance);
        snapshot.setCurrentCents(currentBalance);
        // asOf timestamp will be set automatically by @CreationTimestamp
        
        balanceSnapshotRepository.save(snapshot);
    }

    /**
     * Generate realistic mock transactions for ~90 days
     */
    public void generateMockTransactions(User user) {
        logger.info("Generating mock transactions for user: {}", user.getEmail());
        
        List<Account> accounts = accountRepository.findByUser(user);
        if (accounts.isEmpty()) {
            logger.warn("No accounts found for user: {}", user.getEmail());
            return;
        }
        
        Account checkingAccount = accounts.stream()
            .filter(a -> "checking".equals(a.getSubtype()))
            .findFirst()
            .orElse(accounts.get(0));
            
        Account creditAccount = accounts.stream()
            .filter(a -> "credit_card".equals(a.getSubtype()))
            .findFirst()
            .orElse(null);
        
        Random random = new Random(user.getId()); // Deterministic based on user ID
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);
        
        List<Transaction> transactions = new ArrayList<>();
        
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            boolean isWeekend = date.getDayOfWeek().getValue() >= 6;
            int txnCount = random.nextInt(isWeekend ? 4 : 5) + (isWeekend ? 1 : 3); // 1-4 weekend, 3-7 weekday
            
            for (int i = 0; i < txnCount; i++) {
                Transaction txn = generateRandomTransaction(random, date, checkingAccount, creditAccount);
                if (txn != null && !transactionExists(txn.getExternalId())) {
                    transactions.add(txn);
                }
            }
            
            // Add income deposits (biweekly)
            if (date.getDayOfWeek().getValue() == 5 && date.getDayOfMonth() <= 15) { // First and third Friday
                Transaction income = generateIncomeTransaction(random, date, checkingAccount);
                if (income != null && !transactionExists(income.getExternalId())) {
                    transactions.add(income);
                }
            }
            
            // Add monthly credit card payment
            if (creditAccount != null && date.getDayOfMonth() == 15) {
                Transaction payment = generateCreditPayment(random, date, checkingAccount, creditAccount);
                if (payment != null && !transactionExists(payment.getExternalId())) {
                    transactions.add(payment);
                }
            }
        }
        
        // Detect transfers
        detectTransfers(transactions);
        
        // Save all transactions
        transactionRepository.saveAll(transactions);
        
        logger.info("Generated {} mock transactions for user: {}", transactions.size(), user.getEmail());
    }

    private Transaction generateRandomTransaction(Random random, LocalDate date, Account checkingAccount, Account creditAccount) {
        // Select random merchant
        String merchantKey = MERCHANTS.keySet().toArray(new String[0])[random.nextInt(MERCHANTS.size())];
        TransactionTemplate template = MERCHANTS.get(merchantKey);
        
        // Determine account (70% checking, 30% credit if available)
        Account account = (creditAccount != null && random.nextDouble() < 0.3) ? creditAccount : checkingAccount;
        
        // Generate amount (negative for spending, positive for income)
        long range = template.maxAmountCents - template.minAmountCents;
        Long amountCents = -(template.minAmountCents + (range > 0 ? random.nextLong(range) : 0));
        
        // Create external ID (stable hash to avoid duplicates)
        String externalId = generateExternalId(account.getAccountId(), date, Math.abs(amountCents), merchantKey);
        
        Transaction txn = new Transaction();
        txn.setAccount(account);
        txn.setExternalId(externalId);
        txn.setDate(date);
        txn.setName(merchantKey);
        txn.setMerchantName(merchantKey.toLowerCase().replace(" ", "_"));
        txn.setAmountCents(amountCents);
        txn.setCategoryTop(template.categoryTop);
        txn.setCategorySub(template.categorySub);
        txn.setIsTransfer(false);
        
        return txn;
    }

    private Transaction generateIncomeTransaction(Random random, LocalDate date, Account account) {
        String incomeSource = INCOME_SOURCES.get(random.nextInt(INCOME_SOURCES.size()));
        Long amountCents = 150000L + random.nextLong(200000L + 1); // $1500-3500
        
        String externalId = generateExternalId(account.getAccountId(), date, amountCents, incomeSource);
        
        Transaction txn = new Transaction();
        txn.setAccount(account);
        txn.setExternalId(externalId);
        txn.setDate(date);
        txn.setName(incomeSource);
        txn.setMerchantName("employer");
        txn.setAmountCents(amountCents); // Positive for income
        txn.setCategoryTop("Income");
        txn.setCategorySub("Payroll");
        txn.setIsTransfer(false);
        
        return txn;
    }

    private Transaction generateCreditPayment(Random random, LocalDate date, Account checkingAccount, Account creditAccount) {
        // Payment from checking to credit card
        Long amountCents = -(50000L + random.nextLong(100000L + 1)); // $500-1500 payment
        
        String externalId = generateExternalId(checkingAccount.getAccountId(), date, Math.abs(amountCents), "CREDIT_PAYMENT");
        
        Transaction txn = new Transaction();
        txn.setAccount(checkingAccount);
        txn.setExternalId(externalId);
        txn.setDate(date);
        txn.setName("CREDIT CARD PAYMENT");
        txn.setMerchantName("credit_payment");
        txn.setAmountCents(amountCents); // Negative from checking
        txn.setCategoryTop("Transfer");
        txn.setCategorySub("Credit Card Payment");
        txn.setIsTransfer(true);
        
        return txn;
    }

    /**
     * Detect and mark transfers between accounts
     */
    private void detectTransfers(List<Transaction> transactions) {
        for (int i = 0; i < transactions.size(); i++) {
            Transaction txn1 = transactions.get(i);
            
            for (int j = i + 1; j < transactions.size(); j++) {
                Transaction txn2 = transactions.get(j);
                
                // Check if they're potential transfers
                if (isTransferPair(txn1, txn2)) {
                    txn1.setIsTransfer(true);
                    txn2.setIsTransfer(true);
                    txn1.setCategoryTop("Transfer");
                    txn1.setCategorySub("Account Transfer");
                    txn2.setCategoryTop("Transfer");
                    txn2.setCategorySub("Account Transfer");
                    break; // Found pair, move to next transaction
                }
            }
        }
    }

    private boolean isTransferPair(Transaction txn1, Transaction txn2) {
        // Different accounts
        if (txn1.getAccount().getId().equals(txn2.getAccount().getId())) {
            return false;
        }
        
        // Opposite amounts (within $1)
        if (Math.abs(txn1.getAmountCents() + txn2.getAmountCents()) > 100) {
            return false;
        }
        
        // Within 2 days
        if (Math.abs(txn1.getDate().toEpochDay() - txn2.getDate().toEpochDay()) > 2) {
            return false;
        }
        
        // Contains transfer keywords
        String name1 = txn1.getName().toLowerCase();
        String name2 = txn2.getName().toLowerCase();
        return (name1.contains("payment") || name1.contains("transfer")) || 
               (name2.contains("payment") || name2.contains("transfer"));
    }

    /**
     * Generate stable external ID for deduplication
     */
    private String generateExternalId(String accountId, LocalDate date, Long amount, String description) {
        // Add timestamp and random component to ensure uniqueness across multiple runs
        String input = accountId + ":" + date.toString() + ":" + amount + ":" + 
                      description.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() + ":" +
                      System.nanoTime() + ":" + Math.random();
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return "mock-" + result.toString().substring(0, 16);
        } catch (Exception e) {
            return "mock-" + UUID.randomUUID().toString().substring(0, 16);
        }
    }

    private boolean transactionExists(String externalId) {
        return transactionRepository.findByExternalId(externalId) != null;
    }

    /**
     * Template for generating consistent transaction data
     */
    private static class TransactionTemplate {
        final String categoryTop;
        final String categorySub;
        final Long minAmountCents;
        final Long maxAmountCents;
        
        TransactionTemplate(String categoryTop, String categorySub, long minAmountCents, long maxAmountCents) {
            this.categoryTop = categoryTop;
            this.categorySub = categorySub;
            this.minAmountCents = minAmountCents;
            this.maxAmountCents = maxAmountCents;
        }
    }
}