package com.sanddollar.service;

import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.Account;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Profile("local")
public class BudgetMockDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(BudgetMockDataSeeder.class);
    private static final Long TEST_USER_ID = 9001L;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting BudgetMockDataSeeder for local profile");

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Check if we already have transactions for the current month for our test user
        long existingTransactions = transactionRepository.countByAccountUserIdAndMonth(TEST_USER_ID, currentMonth);

        if (existingTransactions >= 20) { // Threshold for "enough" data
            logger.info("Local profile already has {} transactions for current month, skipping seed", existingTransactions);
            return;
        }

        logger.info("Seeding additional transaction data for local profile testing");

        // Find the test account
        Optional<Account> testAccount = accountRepository.findByUserIdAndAccountId(TEST_USER_ID, "local-account-demo");
        if (testAccount.isEmpty()) {
            logger.warn("Test account not found, unable to seed transactions");
            return;
        }

        Account account = testAccount.get();
        seedCurrentMonthTransactions(account);

        logger.info("BudgetMockDataSeeder completed successfully");
    }

    private void seedCurrentMonthTransactions(Account account) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);

        // Add some additional transactions to ensure we have good test data
        String[] categories = {"Groceries", "Dining", "Transport", "Utilities", "Gym", "Subscriptions", "Misc"};
        int[] amounts = {-4200, -3600, -1200, -1600, -4000, -700, -1300}; // In cents

        for (int i = 0; i < categories.length; i++) {
            for (int week = 0; week < 4 && startOfMonth.plusDays(week * 7).isBefore(now); week++) {
                LocalDate transactionDate = startOfMonth.plusDays(week * 7 + i);
                if (transactionDate.isAfter(now)) break;

                String externalId = String.format("seeded-txn-%s-%s-week%d",
                    categories[i].toLowerCase(),
                    now.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    week);

                // Check if transaction already exists
                if (transactionRepository.findByExternalId(externalId) != null) {
                    continue;
                }

                Transaction transaction = new Transaction();
                transaction.setAccount(account);
                transaction.setExternalId(externalId);
                transaction.setDate(transactionDate);
                transaction.setName(String.format("%s purchase #%d", categories[i], week + 1));
                transaction.setMerchantName(categories[i]);
                transaction.setAmountCents((long) (amounts[i] + (Math.random() * 500 - 250))); // Add some variance
                transaction.setCurrency("USD");
                transaction.setCategoryTop(categories[i]);
                transaction.setCategorySub(categories[i]);
                transaction.setIsTransfer(false);

                transactionRepository.save(transaction);
                logger.debug("Seeded transaction: {} for {}", transaction.getName(), transaction.getDate());
            }
        }
    }
}