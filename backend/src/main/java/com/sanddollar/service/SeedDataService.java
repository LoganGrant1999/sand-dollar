package com.sanddollar.service;

import com.sanddollar.entity.*;
import com.sanddollar.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class SeedDataService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    private static final String[] MERCHANTS = {
        "Whole Foods", "Starbucks", "Shell Gas", "Amazon", "Target", "Uber",
        "Netflix", "Spotify", "McDonald's", "Chipotle", "CVS Pharmacy",
        "Best Buy", "Home Depot", "Costco", "Trader Joe's", "Lyft"
    };

    private static final String[] CATEGORIES = {
        "Food and Drink", "Gas Stations", "Shops", "Recreation", "Transportation",
        "Healthcare", "Service", "Government", "Rent", "Transfer"
    };

    @Override
    @Transactional
    public void run(String... args) {
        if (args.length > 0 && "seed".equals(args[0])) {
            createSeedData();
        }
    }

    public void createSeedData() {
        System.out.println("🌱 Creating seed data for Sand Dollar...");
        
        // Check if demo user already exists
        Optional<User> existingUser = userRepository.findByEmail("demo@sanddollar.app");
        if (existingUser.isPresent()) {
            System.out.println("Demo user already exists, recreating balance snapshots with updated data...");
            User demoUser = existingUser.get();
            
            // Get existing accounts
            List<Account> accounts = accountRepository.findByUser(demoUser);
            
            if (!accounts.isEmpty()) {
                // Clear existing balance snapshots
                for (Account account : accounts) {
                    balanceSnapshotRepository.deleteByAccount(account);
                }
                
                // Create new balance snapshots with 30 days of data
                createBalanceSnapshots(accounts);
                
                System.out.println("✅ Balance snapshots recreated with 30 days of trend data!");
                System.out.println("📧 Demo login: demo@sanddollar.app");
                System.out.println("🔐 Password: Demo123!");
            }
            return;
        }

        // Create demo user
        User demoUser = createDemoUser();
        
        // Create demo accounts
        List<Account> accounts = createDemoAccounts(demoUser);
        
        // Create balance snapshots
        createBalanceSnapshots(accounts);
        
        // Create transactions (60 days worth)
        createTransactions(accounts);
        
        // Create a sample goal
        createSampleGoal(demoUser);
        
        System.out.println("✅ Seed data created successfully!");
        System.out.println("📧 Demo login: demo@sanddollar.app");
        System.out.println("🔐 Password: Demo123!");
    }

    private User createDemoUser() {
        User user = new User();
        user.setEmail("demo@sanddollar.app");
        user.setPasswordHash(passwordEncoder.encode("Demo123!"));
        user.setFirstName("Demo");
        user.setLastName("User");
        return userRepository.save(user);
    }

    private List<Account> createDemoAccounts(User user) {
        List<Account> accounts = new ArrayList<>();
        
        // Checking account
        Account checking = new Account();
        checking.setUser(user);
        checking.setAccountId("demo_checking_001");
        checking.setPlaidAccountId("demo_checking_001");
        checking.setMask("0001");
        checking.setName("Demo Checking");
        checking.setInstitutionName("Demo Bank");
        checking.setType("depository");
        checking.setSubtype("checking");
        accounts.add(accountRepository.save(checking));
        
        // Savings account
        Account savings = new Account();
        savings.setUser(user);
        savings.setAccountId("demo_savings_001");
        savings.setPlaidAccountId("demo_savings_001");
        savings.setMask("0002");
        savings.setName("Demo Savings");
        savings.setInstitutionName("Demo Bank");
        savings.setType("depository");
        savings.setSubtype("savings");
        accounts.add(accountRepository.save(savings));
        
        // Credit card
        Account credit = new Account();
        credit.setUser(user);
        credit.setAccountId("demo_credit_001");
        credit.setPlaidAccountId("demo_credit_001");
        credit.setMask("0003");
        credit.setName("Demo Credit Card");
        credit.setInstitutionName("Demo Bank");
        credit.setType("credit");
        credit.setSubtype("credit_card");
        accounts.add(accountRepository.save(credit));
        
        return accounts;
    }

    private void createBalanceSnapshots(List<Account> accounts) {
        // Create balance snapshots for the last 30 days to show trend data
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        for (Account account : accounts) {
            // Base balance for each account type
            long baseBalance;
            long baseAvailable;
            
            switch (account.getSubtype()) {
                case "checking":
                    baseBalance = 285000L; // $2,850
                    baseAvailable = 285000L;
                    break;
                case "savings":
                    baseBalance = 1250000L; // $12,500
                    baseAvailable = 1250000L;
                    break;
                case "credit_card":
                    baseBalance = -125000L; // -$1,250 (balance owed)
                    baseAvailable = 375000L; // $3,750 available credit
                    break;
                default:
                    baseBalance = 100000L;
                    baseAvailable = 100000L;
            }
            
            // Create daily balance snapshots with some realistic variation
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                BalanceSnapshot snapshot = new BalanceSnapshot();
                snapshot.setAccount(account);
                snapshot.setCurrency("USD");
                
                // Add some realistic daily variation to balances
                long variation = (random.nextLong(20000L) - 10000L); // ±$100 daily variation
                long dailyBalance = baseBalance + variation;
                
                // For credit cards, keep the negative balance logic
                if ("credit_card".equals(account.getSubtype())) {
                    snapshot.setCurrentCents(dailyBalance);
                    snapshot.setAvailableCents(baseAvailable);
                } else {
                    // For depository accounts, don't let balance go negative
                    snapshot.setCurrentCents(Math.max(dailyBalance, 10000L)); // At least $100
                    snapshot.setAvailableCents(Math.max(dailyBalance, 10000L));
                }
                
                // Set the timestamp to the specific date
                snapshot.setAsOf(date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
                
                balanceSnapshotRepository.save(snapshot);
            }
        }
    }

    private void createTransactions(List<Account> accounts) {
        Account checking = accounts.get(0);
        Account savings = accounts.get(1);
        Account credit = accounts.get(2);
        
        LocalDate startDate = LocalDate.now().minusDays(60);
        LocalDate endDate = LocalDate.now();
        
        List<Transaction> transactions = new ArrayList<>();
        
        // Create monthly income
        for (int month = 0; month < 3; month++) {
            LocalDate payDate = startDate.plusDays(month * 30 + 15);
            if (!payDate.isAfter(endDate)) {
                Transaction salary = createTransaction(
                    checking, payDate, "Direct Deposit - Salary", "ACME Corp",
                    500000L, "Income", "Payroll"
                );
                transactions.add(salary);
            }
        }
        
        // Create recurring expenses
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // Monthly bills (1st of month)
            if (date.getDayOfMonth() == 1) {
                transactions.add(createTransaction(checking, date, "Rent Payment", "Property Management", -180000L, "Payment", "Rent"));
                transactions.add(createTransaction(checking, date, "Electric Bill", "City Electric", -8500L, "Service", "Utilities"));
                transactions.add(createTransaction(checking, date, "Internet", "Fiber Co", -6999L, "Service", "Internet"));
            }
            
            // Subscription services
            if (date.getDayOfMonth() == 15) {
                transactions.add(createTransaction(checking, date, "Netflix", "Netflix", -1599L, "Service", "Entertainment"));
                transactions.add(createTransaction(checking, date, "Spotify Premium", "Spotify", -999L, "Service", "Entertainment"));
            }
            
            // Random daily spending
            if (random.nextDouble() < 0.7) { // 70% chance of spending each day
                String merchant = MERCHANTS[random.nextInt(MERCHANTS.length)];
                String category = getCategoryForMerchant(merchant);
                long amount = generateRealisticAmount(merchant);
                
                Account account = merchant.equals("Amazon") || random.nextDouble() < 0.3 ? credit : checking;
                transactions.add(createTransaction(account, date, merchant, merchant, -amount, category, category));
            }
            
            // Occasional savings transfer
            if (random.nextDouble() < 0.1) { // 10% chance
                long transferAmount = 25000L + random.nextLong(50000L); // $250-$750
                transactions.add(createTransaction(checking, date, "Transfer to Savings", null, -transferAmount, "Transfer", "Internal"));
                transactions.add(createTransaction(savings, date, "Transfer from Checking", null, transferAmount, "Transfer", "Internal"));
            }
        }
        
        // Mark transfers
        markTransfers(transactions);
        
        // Save all transactions
        transactionRepository.saveAll(transactions);
    }

    private Transaction createTransaction(Account account, LocalDate date, String name, 
                                       String merchant, Long amountCents, String categoryTop, String categorySub) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setExternalId("demo_" + date.toString() + "_" + System.nanoTime());
        transaction.setPlaidTransactionId(transaction.getExternalId());
        transaction.setDate(date);
        transaction.setName(name);
        transaction.setMerchantName(merchant);
        transaction.setAmountCents(amountCents);
        transaction.setCurrency("USD");
        transaction.setCategoryTop(categoryTop);
        transaction.setCategorySub(categorySub);
        return transaction;
    }

    private String getCategoryForMerchant(String merchant) {
        return switch (merchant) {
            case "Whole Foods", "Trader Joe's", "Costco" -> "Food and Drink";
            case "Starbucks", "McDonald's", "Chipotle" -> "Food and Drink";
            case "Shell Gas" -> "Transportation";
            case "Amazon", "Target", "Best Buy", "Home Depot" -> "Shops";
            case "Uber", "Lyft" -> "Transportation";
            case "Netflix", "Spotify" -> "Service";
            case "CVS Pharmacy" -> "Healthcare";
            default -> "General Merchandise";
        };
    }

    private long generateRealisticAmount(String merchant) {
        return switch (merchant) {
            case "Starbucks" -> 500L + random.nextLong(800L); // $5-13
            case "Shell Gas" -> 3000L + random.nextLong(5000L); // $30-80
            case "Whole Foods", "Trader Joe's" -> 2500L + random.nextLong(12000L); // $25-145
            case "McDonald's", "Chipotle" -> 800L + random.nextLong(1500L); // $8-23
            case "Uber", "Lyft" -> 1200L + random.nextLong(2800L); // $12-40
            case "Amazon" -> 1500L + random.nextLong(15000L); // $15-165
            case "Target", "Costco" -> 2000L + random.nextLong(18000L); // $20-200
            case "Netflix" -> 1599L; // $15.99
            case "Spotify" -> 999L; // $9.99
            default -> 1000L + random.nextLong(5000L); // $10-60
        };
    }

    private void markTransfers(List<Transaction> transactions) {
        for (Transaction t : transactions) {
            if ("Transfer".equals(t.getCategoryTop()) || 
                (t.getName() != null && t.getName().toLowerCase().contains("transfer"))) {
                t.setIsTransfer(true);
            }
        }
    }

    private void createSampleGoal(User user) {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setName("Emergency Fund");
        goal.setTargetCents(1000000L); // $10,000
        goal.setSavedCents(250000L); // $2,500 saved so far
        goal.setTargetDate(LocalDate.now().plusMonths(8));
        goal.setStatus(Goal.GoalStatus.ACTIVE);
        goalRepository.save(goal);
    }
}
