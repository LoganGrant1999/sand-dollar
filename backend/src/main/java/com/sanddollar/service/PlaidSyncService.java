package com.sanddollar.service;

import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountsGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.PersonalFinanceCategory;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.TransactionCode;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncRequestOptions;
import com.plaid.client.model.TransactionsSyncResponse;
import com.plaid.client.request.PlaidApi;
import com.sanddollar.config.PlaidConfig;
import com.sanddollar.entity.Account;
import com.sanddollar.entity.PlaidItem;
import com.sanddollar.entity.Transaction;
import com.sanddollar.entity.User;
import com.sanddollar.repository.AccountRepository;
import com.sanddollar.repository.PlaidItemRepository;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Profile("plaid")
@Transactional
public class PlaidSyncService {
    private static final Logger logger = LoggerFactory.getLogger(PlaidSyncService.class);
    private static final int PAGE_SIZE = 100;

    private final PlaidApi plaidApi;
    private final PlaidConfig plaidConfig;
    private final UserRepository userRepository;
    private final PlaidItemRepository plaidItemRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CryptoService cryptoService;
    private final PlaidCategoryMapper categoryMapper;

    public PlaidSyncService(
            PlaidApi plaidApi,
            PlaidConfig plaidConfig,
            UserRepository userRepository,
            PlaidItemRepository plaidItemRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            CryptoService cryptoService,
            PlaidCategoryMapper categoryMapper) {
        this.plaidApi = plaidApi;
        this.plaidConfig = plaidConfig;
        this.userRepository = userRepository;
        this.plaidItemRepository = plaidItemRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.cryptoService = cryptoService;
        this.categoryMapper = categoryMapper;
    }

    public SyncResult initialBackfill(Long userId) {
        logger.info("Starting Plaid initial backfill for user {}", userId);
        return syncUserItems(userId, true);
    }

    public SyncResult incrementalSync(Long userId) {
        logger.info("Running Plaid incremental sync for user {}", userId);
        return syncUserItems(userId, false);
    }

    private SyncResult syncUserItems(Long userId, boolean resetCursor) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        validateCredentials();

        List<PlaidItem> items = plaidItemRepository.findByUserAndStatus(user, PlaidItem.PlaidItemStatus.ACTIVE);
        int accountUpserts = 0;
        int transactionUpserts = 0;

        for (PlaidItem item : items) {
            SyncResult result = syncItem(item, resetCursor);
            accountUpserts += result.accountsUpserted();
            transactionUpserts += result.transactionsUpserted();
        }

        return new SyncResult(accountUpserts, transactionUpserts);
    }

    private SyncResult syncItem(PlaidItem plaidItem, boolean resetCursor) {
        String decryptedToken = decryptToken(plaidItem);
        String cursor = resetCursor ? null : plaidItem.getCursor();
        boolean hasMore = true;
        int accountsUpdated = 0;
        int transactionsUpdated = 0;
        Map<String, Account> accountCache = new HashMap<>();

        accountsUpdated += refreshAccountsForItem(plaidItem, decryptedToken, accountCache);

        while (hasMore) {
            TransactionsSyncRequest request = new TransactionsSyncRequest()
                .accessToken(decryptedToken)
                .clientId(plaidConfig.getClientId())
                .secret(plaidConfig.getSecret())
                .cursor(cursor)
                .count(PAGE_SIZE)
                .options(new TransactionsSyncRequestOptions()
                    .includePersonalFinanceCategory(Boolean.TRUE)
                    .includeOriginalDescription(Boolean.TRUE)
                );

            TransactionsSyncResponse response = executeCall(plaidApi.transactionsSync(request));

            transactionsUpdated += upsertTransactions(accountCache, response.getAdded());
            transactionsUpdated += upsertTransactions(accountCache, response.getModified());
            removeTransactions(response.getRemoved());

            cursor = response.getNextCursor();
            hasMore = Boolean.TRUE.equals(response.getHasMore());
        }

        plaidItem.setCursor(cursor);
        plaidItemRepository.save(plaidItem);

        return new SyncResult(accountsUpdated, transactionsUpdated);
    }

    private int refreshAccountsForItem(PlaidItem plaidItem, String accessToken, Map<String, Account> cache) {
        AccountsGetRequest request = new AccountsGetRequest()
            .accessToken(accessToken)
            .clientId(plaidConfig.getClientId())
            .secret(plaidConfig.getSecret());

        AccountsGetResponse response = executeCall(plaidApi.accountsGet(request));
        int upserts = 0;

        if (response.getAccounts() == null) {
            return 0;
        }

        for (AccountBase plaidAccount : response.getAccounts()) {
            String plaidAccountId = plaidAccount.getAccountId();
            Account account = accountRepository.findByPlaidAccountId(plaidAccountId)
                .orElseGet(Account::new);

            boolean isNew = account.getId() == null;
            boolean changed = applyAccountUpdates(account, plaidItem, plaidAccount);
            if (isNew || changed) {
                accountRepository.save(account);
                upserts++;
            }
            cache.put(plaidAccountId, account);
        }

        return upserts;
    }

    private boolean applyAccountUpdates(Account account, PlaidItem plaidItem, AccountBase plaidAccount) {
        boolean changed = false;

        if (!Objects.equals(account.getUser(), plaidItem.getUser())) {
            account.setUser(plaidItem.getUser());
            changed = true;
        }
        if (!Objects.equals(account.getPlaidItem(), plaidItem)) {
            account.setPlaidItem(plaidItem);
            changed = true;
        }

        String plaidAccountId = plaidAccount.getAccountId();
        if (plaidAccountId != null && !plaidAccountId.equals(account.getPlaidAccountId())) {
            account.setPlaidAccountId(plaidAccountId);
            account.setAccountId(plaidAccountId);
            changed = true;
        }

        String mask = plaidAccount.getMask();
        if (!Objects.equals(account.getMask(), mask)) {
            account.setMask(mask);
            changed = true;
        }

        String name = plaidAccount.getName();
        if (!Objects.equals(account.getName(), name)) {
            account.setName(name);
            changed = true;
        }

        if (!Objects.equals(account.getInstitutionName(), plaidItem.getInstitutionName())) {
            account.setInstitutionName(plaidItem.getInstitutionName());
            changed = true;
        }

        String type = plaidAccount.getType() != null ? plaidAccount.getType().getValue() : null;
        if (!Objects.equals(account.getType(), type)) {
            account.setType(type != null ? type : "other");
            changed = true;
        }

        String subtype = plaidAccount.getSubtype() != null ? plaidAccount.getSubtype().getValue() : null;
        if (!Objects.equals(account.getSubtype(), subtype)) {
            account.setSubtype(subtype);
            changed = true;
        }

        return changed;
    }

    private int upsertTransactions(Map<String, Account> accountCache, List<com.plaid.client.model.Transaction> plaidTransactions) {
        if (plaidTransactions == null || plaidTransactions.isEmpty()) {
            return 0;
        }

        int upserts = 0;
        for (com.plaid.client.model.Transaction plaidTxn : plaidTransactions) {
            String accountId = plaidTxn.getAccountId();
            if (accountId == null) {
                continue;
            }

            Account account = accountCache.computeIfAbsent(accountId, id ->
                accountRepository.findByPlaidAccountId(id).orElse(null)
            );

            if (account == null) {
                logger.warn("Skipping transaction {} because account {} was not found", plaidTxn.getTransactionId(), accountId);
                continue;
            }

            if (upsertTransaction(account, plaidTxn)) {
                upserts++;
            }
        }
        return upserts;
    }

    private boolean upsertTransaction(Account account, com.plaid.client.model.Transaction plaidTxn) {
        String plaidTransactionId = plaidTxn.getTransactionId();
        if (plaidTransactionId == null) {
            return false;
        }

        boolean pending = Boolean.TRUE.equals(plaidTxn.getPending());
        String pendingTransactionId = plaidTxn.getPendingTransactionId();

        Optional<Transaction> existing = transactionRepository.findByPlaidTransactionId(plaidTransactionId);
        Transaction entity = existing.orElse(null);

        if (entity == null && !pending && pendingTransactionId != null) {
            entity = transactionRepository.findByPlaidTransactionId(pendingTransactionId)
                .orElseGet(() -> transactionRepository.findByPendingTransactionId(pendingTransactionId).orElse(null));
        }

        boolean isNew = false;
        if (entity == null) {
            entity = new Transaction();
            entity.setAccount(account);
            entity.setExternalId(plaidTransactionId);
            entity.setPlaidTransactionId(plaidTransactionId);
            isNew = true;
        }

        entity.setAccount(account);
        entity.setExternalId(plaidTransactionId);
        entity.setPlaidTransactionId(plaidTransactionId);
        entity.setPendingTransactionId(pendingTransactionId);
        entity.setPending(pending);

        LocalDate date = plaidTxn.getDate();
        if (date != null) {
            entity.setDate(date);
        }

        entity.setName(plaidTxn.getName());
        entity.setMerchantName(plaidTxn.getMerchantName());

        Long amountCents = normalizeAmount(plaidTxn.getAmount());
        entity.setAmountCents(amountCents);
        entity.setCurrency(plaidTxn.getIsoCurrencyCode());

        PersonalFinanceCategory pfc = plaidTxn.getPersonalFinanceCategory();
        PlaidCategoryMapper.CategoryMapping mapping = categoryMapper.mapCategory(pfc);
        entity.setCategoryTop(mapping.primary());
        entity.setCategorySub(mapping.secondary());
        entity.setIsTransfer(isLikelyTransfer(plaidTxn, pfc));

        transactionRepository.save(entity);
        return true;
    }

    private void removeTransactions(List<RemovedTransaction> removedTransactions) {
        if (removedTransactions == null || removedTransactions.isEmpty()) {
            return;
        }

        for (RemovedTransaction removed : removedTransactions) {
            String transactionId = removed.getTransactionId();
            if (transactionId == null) {
                continue;
            }
            transactionRepository.findByPlaidTransactionId(transactionId)
                .ifPresent(transactionRepository::delete);
        }
    }

    private boolean isLikelyTransfer(com.plaid.client.model.Transaction plaidTxn, PersonalFinanceCategory category) {
        if (category != null && "TRANSFER".equalsIgnoreCase(category.getPrimary())) {
            return true;
        }
        TransactionCode transactionCode = plaidTxn.getTransactionCode();
        return transactionCode != null && "TRANSFER".equalsIgnoreCase(transactionCode.getValue());
    }

    private Long normalizeAmount(Double amount) {
        if (amount == null) {
            return 0L;
        }
        long cents = Math.round(Math.abs(amount) * 100);
        return amount > 0 ? -cents : cents;
    }

    private <T> T executeCall(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful() || response.body() == null) {
                String error = response.errorBody() != null ? response.errorBody().string() : "unknown";
                throw new IllegalStateException("Plaid request failed: HTTP " + response.code() + " - " + error);
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Plaid API request failed", e);
        }
    }

    private String decryptToken(PlaidItem plaidItem) {
        try {
            return cryptoService.decrypt(plaidItem.getAccessTokenEncrypted());
        } catch (Exception e) {
            logger.warn("Failed to decrypt access token for item {}. Assuming token stored in plain text.", plaidItem.getItemId(), e);
            return plaidItem.getAccessTokenEncrypted();
        }
    }

    private void validateCredentials() {
        if (plaidConfig.getClientId() == null || plaidConfig.getClientId().isBlank()) {
            throw new IllegalStateException("PLAID_CLIENT_ID is not configured");
        }
        if (plaidConfig.getSecret() == null || plaidConfig.getSecret().isBlank()) {
            throw new IllegalStateException("PLAID_SECRET is not configured");
        }
    }

    public record SyncResult(int accountsUpserted, int transactionsUpserted) { }
}
