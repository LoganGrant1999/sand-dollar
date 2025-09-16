package com.sanddollar.service;

import com.sanddollar.entity.User;
import com.sanddollar.entity.PlaidItem;
import com.sanddollar.entity.Account;
import com.sanddollar.entity.BalanceSnapshot;
import com.sanddollar.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Profile("plaid")
@Transactional
public class PlaidService implements BankDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(PlaidService.class);

    @Autowired
    private PlaidItemRepository plaidItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public String createLinkToken(User user) {
        // Return a mock link token for demo purposes
        logger.info("Creating Plaid link token for user: {}", user.getEmail());
        return "link-sandbox-demo-token-" + UUID.randomUUID().toString();
    }

    @Override
    public PlaidItem exchangePublicToken(User user, String publicToken) {
        // Create a mock PlaidItem for demo
        logger.info("Exchanging Plaid public token for user: {}", user.getEmail());
        PlaidItem plaidItem = new PlaidItem();
        plaidItem.setUser(user);
        plaidItem.setItemId("demo-item-" + UUID.randomUUID().toString());
        plaidItem.setAccessTokenEncrypted("demo-token-encrypted");
        plaidItem.setInstitutionName("Demo Bank");
        plaidItem.setCreatedAt(Instant.now());
        
        PlaidItem savedItem = plaidItemRepository.save(plaidItem);
        
        // Sync demo accounts immediately
        syncAccounts(savedItem);
        
        return savedItem;
    }

    public void syncAccounts(PlaidItem plaidItem) {
        logger.info("Syncing accounts for item: {} (demo mode)", plaidItem.getItemId());
        // Demo implementation - create mock accounts if none exist
        List<Account> existingAccounts = accountRepository.findByUser(plaidItem.getUser());
        if (existingAccounts.isEmpty()) {
            // Create demo accounts
            createDemoAccount(plaidItem, "Demo Checking", "depository", "checking");
            createDemoAccount(plaidItem, "Demo Savings", "depository", "savings");
        }
    }

    private void createDemoAccount(PlaidItem plaidItem, String name, String type, String subtype) {
        Account account = new Account();
        account.setUser(plaidItem.getUser());
        account.setPlaidItem(plaidItem);
        account.setAccountId("demo-" + UUID.randomUUID().toString());
        account.setName(name);
        account.setInstitutionName("Demo Bank");
        account.setType(type);
        account.setSubtype(subtype);
        account.setCreatedAt(Instant.now());
        
        accountRepository.save(account);
    }

    public void syncBalances(PlaidItem plaidItem) {
        logger.info("Syncing balances for item: {} (demo mode)", plaidItem.getItemId());
        // Demo implementation
        List<Account> accounts = accountRepository.findByUser(plaidItem.getUser());
        for (Account account : accounts) {
            BalanceSnapshot snapshot = new BalanceSnapshot();
            snapshot.setAccount(account);
            
            // Generate demo balances
            Random random = new Random();
            long availableBalance = random.nextLong(100000, 500000); // $1000 to $5000
            long currentBalance = availableBalance + random.nextLong(0, 10000); // Slightly higher
            
            snapshot.setAvailableCents(availableBalance);
            snapshot.setCurrentCents(currentBalance);
            
            balanceSnapshotRepository.save(snapshot);
        }
    }

    public void syncTransactions(PlaidItem plaidItem, LocalDate startDate, LocalDate endDate) {
        logger.info("Syncing transactions for item: {} from {} to {} (demo mode)", 
                   plaidItem.getItemId(), startDate, endDate);
        // Demo implementation - would sync real transactions in production
    }

    @Override
    public Map<String, Object> fetchBalances(User user) {
        logger.info("Fetching Plaid balances for user: {}", user.getEmail());
        List<PlaidItem> plaidItems = plaidItemRepository.findByUser(user);
        
        for (PlaidItem plaidItem : plaidItems) {
            syncBalances(plaidItem);
        }
        
        // Return summary of balances
        List<BalanceSnapshot> recentBalances = balanceSnapshotRepository.findRecentByUser(user);
        Long totalAvailable = recentBalances.stream()
            .mapToLong(b -> b.getAvailableCents() != null ? b.getAvailableCents() : 0L)
            .sum();
        
        return Map.of(
            "totalAvailableCents", totalAvailable,
            "asOf", Instant.now()
        );
    }

    @Override
    public Map<String, Object> syncTransactions(User user, String cursor) {
        logger.info("Syncing Plaid transactions for user: {}", user.getEmail());
        List<PlaidItem> plaidItems = plaidItemRepository.findByUser(user);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        for (PlaidItem plaidItem : plaidItems) {
            syncTransactions(plaidItem, startDate, endDate);
            if (cursor != null) {
                plaidItem.setCursor(cursor);
            }
        }

        return Map.of(
            "success", true,
            "message", "Plaid transactions synced"
        );
    }

    @Override
    public void handleWebhook(String itemId, String webhookType, String webhookCode) {
        logger.info("Received webhook - Item: {}, Type: {}, Code: {} (demo mode)", itemId, webhookType, webhookCode);
        // Demo implementation - would handle real webhooks in production
    }
    
    // Legacy method for backward compatibility
    public Map<String, Object> refreshBalances(User user) {
        return fetchBalances(user);
    }
}
