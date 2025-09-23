package com.sanddollar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountsGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.Institution;
import com.plaid.client.model.InstitutionsGetByIdRequest;
import com.plaid.client.model.InstitutionsGetByIdResponse;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;
import com.sanddollar.config.PlaidConfig;
import com.sanddollar.entity.Account;
import com.sanddollar.entity.BalanceSnapshot;
import com.sanddollar.entity.PlaidItem;
import com.sanddollar.entity.User;
import com.sanddollar.repository.AccountRepository;
import com.sanddollar.repository.BalanceSnapshotRepository;
import com.sanddollar.repository.PlaidItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Response;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("plaid")
@Transactional
public class PlaidService implements BankDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(PlaidService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PlaidApi plaidApi;
    private final PlaidConfig plaidConfig;
    private final CryptoService cryptoService;
    private final PlaidItemRepository plaidItemRepository;
    private final AccountRepository accountRepository;
    private final BalanceSnapshotRepository balanceSnapshotRepository;
    private final PlaidSyncService plaidSyncService;

    public PlaidService(
            PlaidApi plaidApi,
            PlaidConfig plaidConfig,
            CryptoService cryptoService,
            PlaidItemRepository plaidItemRepository,
            AccountRepository accountRepository,
            BalanceSnapshotRepository balanceSnapshotRepository,
            PlaidSyncService plaidSyncService) {
        this.plaidApi = plaidApi;
        this.plaidConfig = plaidConfig;
        this.cryptoService = cryptoService;
        this.plaidItemRepository = plaidItemRepository;
        this.accountRepository = accountRepository;
        this.balanceSnapshotRepository = balanceSnapshotRepository;
        this.plaidSyncService = plaidSyncService;
    }

    @Override
    public String createLinkToken(User user) {
        logger.info("Creating Plaid link token for user: {}", user.getEmail());

        LinkTokenCreateRequestUser linkUser = new LinkTokenCreateRequestUser()
            .clientUserId(user.getId().toString());

        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
            .clientName("Sand Dollar")
            .user(linkUser)
            .products(List.of(Products.TRANSACTIONS))
            .countryCodes(List.of(CountryCode.US))
            .language("en");

        if (plaidConfig.getRedirectUri() != null && !plaidConfig.getRedirectUri().isBlank()) {
            request.setRedirectUri(plaidConfig.getRedirectUri());
        }

        try {
            Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                PlaidError plaidError = readPlaidError(response);
                logger.error("Plaid link_token_create failed: type={} code={} message={}",
                    plaidError.type(), plaidError.code(), plaidError.message());
                throw new PlaidApiException(response.code(), plaidError);
            }
            return response.body().getLinkToken();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Plaid link token", e);
        }
    }

    @Override
    public PlaidItem exchangePublicToken(User user, String publicToken) {
        logger.info("Exchanging Plaid public token for user: {}", user.getEmail());
        try {
            Response<ItemPublicTokenExchangeResponse> response = plaidApi
                .itemPublicTokenExchange(new ItemPublicTokenExchangeRequest().publicToken(publicToken))
                .execute();

            if (!response.isSuccessful() || response.body() == null) {
                PlaidError plaidError = readPlaidError(response);
                logger.error("Plaid token exchange failed: type={} code={} message={}",
                    plaidError.type(), plaidError.code(), plaidError.message());
                throw new PlaidApiException(response.code(), plaidError);
            }

            ItemPublicTokenExchangeResponse body = response.body();
            String accessToken = body.getAccessToken();

            // Check for existing connection to prevent duplicates
            PlaidItem existingItem = plaidItemRepository.findByItemId(body.getItemId()).orElse(null);
            if (existingItem != null && existingItem.getUser().equals(user)) {
                logger.warn("User {} already has a connection for itemId: {}", user.getEmail(), body.getItemId());
                throw new RuntimeException("Account(s) already connected!");
            }

            // Get institution information before saving
            AccountsGetRequest accountsRequest = new AccountsGetRequest().accessToken(accessToken);
            Response<AccountsGetResponse> accountsResponse = plaidApi.accountsGet(accountsRequest).execute();

            if (!accountsResponse.isSuccessful() || accountsResponse.body() == null) {
                PlaidError plaidError = readPlaidError(accountsResponse);
                logger.error("Failed to get institution info: type={} code={} message={}",
                    plaidError.type(), plaidError.code(), plaidError.message());
                throw new PlaidApiException(accountsResponse.code(), plaidError);
            }

            // Allow multiple accounts per bank, but prevent duplicate item connections
            String institutionId = accountsResponse.body().getItem().getInstitutionId();

            String encryptedToken = encrypt(accessToken);
            String institutionName = getInstitutionName(institutionId);

            PlaidItem plaidItem = new PlaidItem();
            plaidItem.setUser(user);
            plaidItem.setItemId(body.getItemId());
            plaidItem.setAccessTokenEncrypted(encryptedToken);
            plaidItem.setInstitutionId(institutionId);
            plaidItem.setInstitutionName(institutionName);

            PlaidItem savedItem = plaidItemRepository.save(plaidItem);

            upsertAccounts(savedItem, accessToken, Instant.now());

            return savedItem;
        } catch (IOException e) {
            throw new RuntimeException("Failed to exchange Plaid public token", e);
        }
    }

    @Override
    public Map<String, Object> fetchBalances(User user) {
        List<PlaidItem> items = plaidItemRepository.findByUser(user);
        long totalAvailableCents = 0L;
        Instant asOf = Instant.now();

        for (PlaidItem item : items) {
            String accessToken = decrypt(item.getAccessTokenEncrypted());
            totalAvailableCents += upsertAccounts(item, accessToken, asOf);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("totalAvailableCents", totalAvailableCents);
        payload.put("asOf", asOf);
        return payload;
    }

    @Override
    public Map<String, Object> syncTransactions(User user, String cursor) {
        PlaidSyncService.SyncResult result = plaidSyncService.incrementalSync(user.getId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("accountsUpserted", result.accountsUpserted());
        payload.put("transactionsUpserted", result.transactionsUpserted());
        payload.put("message", "Plaid transactions synced");
        if (cursor != null) {
            payload.put("receivedCursor", cursor);
        }
        return payload;
    }

    @Override
    public void handleWebhook(String itemId, String webhookType, String webhookCode) {
        logger.info("Received Plaid webhook: item={}, type={}, code={}", itemId, webhookType, webhookCode);
    }

    public Map<String, Object> refreshBalances(User user) {
        return fetchBalances(user);
    }

    private String getInstitutionName(String institutionId) {
        try {
            InstitutionsGetByIdRequest request = new InstitutionsGetByIdRequest()
                .institutionId(institutionId)
                .countryCodes(List.of(CountryCode.US));

            Response<InstitutionsGetByIdResponse> response = plaidApi.institutionsGetById(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                Institution institution = response.body().getInstitution();
                return institution.getName();
            } else {
                logger.warn("Failed to get institution name for ID: {}", institutionId);
                return institutionId; // Fallback to ID
            }
        } catch (IOException e) {
            logger.warn("Error getting institution name for ID: {}", institutionId, e);
            return institutionId; // Fallback to ID
        }
    }

    private long upsertAccounts(PlaidItem plaidItem, String accessToken, Instant asOf) {
        try {
            AccountsGetRequest request = new AccountsGetRequest()
                .accessToken(accessToken);

            Response<AccountsGetResponse> response = plaidApi.accountsGet(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                PlaidError plaidError = readPlaidError(response);
                logger.error("Plaid accounts/get failed: type={} code={} message={}",
                    plaidError.type(), plaidError.code(), plaidError.message());
                throw new PlaidApiException(response.code(), plaidError);
            }

            AccountsGetResponse body = response.body();
            if (body.getItem() != null) {
                if (plaidItem.getInstitutionId() == null) {
                    plaidItem.setInstitutionId(body.getItem().getInstitutionId());
                }
                if (plaidItem.getInstitutionName() == null) {
                    String institutionName = getInstitutionName(body.getItem().getInstitutionId());
                    plaidItem.setInstitutionName(institutionName);
                }
                plaidItemRepository.save(plaidItem);
            }

            long totalAvailableCents = 0L;
            for (AccountBase accountBase : body.getAccounts()) {
                Account account = accountRepository.findByPlaidAccountId(accountBase.getAccountId())
                    .orElseGet(Account::new);

                account.setPlaidItem(plaidItem);
                account.setUser(plaidItem.getUser());
                account.setPlaidAccountId(accountBase.getAccountId());
                account.setAccountId(Optional.ofNullable(accountBase.getAccountId()).orElse(UUID.randomUUID().toString()));
                account.setMask(accountBase.getMask());
                account.setName(accountBase.getName());
                account.setInstitutionName(plaidItem.getInstitutionName());
                account.setType(accountBase.getType() != null ? accountBase.getType().getValue() : null);
                account.setSubtype(accountBase.getSubtype() != null ? accountBase.getSubtype().getValue() : null);
                accountRepository.save(account);

                AccountBalance balance = accountBase.getBalances();
                if (balance != null) {
                    Long availableCents = convertToCents(balance.getAvailable());
                    Long currentCents = convertToCents(balance.getCurrent());

                    BalanceSnapshot snapshot = new BalanceSnapshot();
                    snapshot.setAccount(account);
                    snapshot.setAsOf(asOf);
                    snapshot.setAvailableCents(availableCents);
                    snapshot.setCurrentCents(currentCents != null ? currentCents : availableCents);
                    snapshot.setCurrency(balance.getIsoCurrencyCode());
                    balanceSnapshotRepository.save(snapshot);

                    if (availableCents != null) {
                        totalAvailableCents += availableCents;
                    }
                }
            }

            return totalAvailableCents;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update Plaid accounts", e);
        }
    }

    private Long convertToCents(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100);
    }

    private PlaidError readPlaidError(Response<?> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : null;
            if (errorBody == null || errorBody.isBlank()) {
                return new PlaidError("unknown", "unknown_error", "Plaid request failed with status " + response.code());
            }
            JsonNode node = OBJECT_MAPPER.readTree(errorBody);
            String type = node.path("error_type").asText("unknown_error");
            String code = node.path("error_code").asText("unknown");
            String message = node.path("error_message").asText("Plaid request failed with status " + response.code());
            return new PlaidError(code, type, message);
        } catch (IOException jsonException) {
            logger.warn("Failed to parse Plaid error body", jsonException);
            return new PlaidError("unknown", "parse_error", "Plaid request failed with status " + response.code());
        }
    }

    private String encrypt(String raw) {
        try {
            return cryptoService.encrypt(raw);
        } catch (Exception e) {
            logger.warn("Failed to encrypt Plaid access token. Falling back to plain text storage. Ensure ENCRYPTION_KEY is set.", e);
            return raw;
        }
    }

    private String decrypt(String encrypted) {
        try {
            return cryptoService.decrypt(encrypted);
        } catch (Exception e) {
            logger.warn("Failed to decrypt Plaid access token. Assuming token stored in plain text.", e);
            return encrypted;
        }
    }

    public record PlaidError(String code, String type, String message) {}

    public static class PlaidApiException extends RuntimeException {
        private final int status;
        private final PlaidError error;

        public PlaidApiException(int status, PlaidError error) {
            super(error.message());
            this.status = status;
            this.error = error;
        }

        public int status() {
            return status;
        }

        public PlaidError error() {
            return error;
        }
    }
}
