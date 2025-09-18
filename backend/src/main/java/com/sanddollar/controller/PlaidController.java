package com.sanddollar.controller;

import com.sanddollar.dto.BalancesResponse;
import com.sanddollar.dto.PlaidExchangeRequest;
import com.sanddollar.dto.PlaidTransactionsSyncRequest;
import com.sanddollar.entity.PlaidItem;
import com.sanddollar.entity.User;
import com.sanddollar.repository.PlaidItemRepository;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.BankDataProvider;
import com.sanddollar.service.PlaidService.PlaidApiException;
import com.sanddollar.service.PlaidService.PlaidError;
import com.sanddollar.service.PlaidSyncService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/plaid")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class PlaidController {

    private static final Logger logger = LoggerFactory.getLogger(PlaidController.class);

    @Autowired(required = false)
    private BankDataProvider bankDataProvider;

    @Autowired(required = false)
    private PlaidItemRepository plaidItemRepository;

    @Autowired(required = false)
    private PlaidSyncService plaidSyncService;

    private User requireUser(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException();
        }
        return principal.getUser();
    }

    @PostMapping({"/link-token", "/link/token/create"})
    public ResponseEntity<?> createLinkToken(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (bankDataProvider == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Plaid service not available"));
        }
        try {
            User user = requireUser(userPrincipal);
            String linkToken = bankDataProvider.createLinkToken(user);
            return ResponseEntity.ok(Map.of("link_token", linkToken));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        } catch (PlaidApiException ex) {
            PlaidError error = ex.error();
            logger.warn("Plaid link token failed: type={} code={} message={}", error.type(), error.code(), error.message());
            return ResponseEntity.status(ex.status())
                .body(Map.of(
                    "code", error.code(),
                    "type", error.type(),
                    "message", error.message()
                ));
        } catch (Exception e) {
            logger.error("Unexpected error creating Plaid link token", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to create link token"));
        }
    }

    @PostMapping({"/exchange", "/item/public_token/exchange"})
    public ResponseEntity<?> exchangePublicToken(
            @Valid @RequestBody PlaidExchangeRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (bankDataProvider == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Plaid service not available"));
        }
        try {
            User user = requireUser(userPrincipal);
            PlaidItem plaidItem = bankDataProvider.exchangePublicToken(user, request.publicToken());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "item_id", plaidItem.getItemId(),
                "institution_name", plaidItem.getInstitutionName()
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        } catch (PlaidApiException ex) {
            PlaidError error = ex.error();
            logger.warn("Plaid token exchange failed: type={} code={} message={}", error.type(), error.code(), error.message());
            return ResponseEntity.status(ex.status())
                .body(Map.of(
                    "code", error.code(),
                    "type", error.type(),
                    "message", error.message()
                ));
        } catch (Exception e) {
            logger.error("Unexpected error exchanging Plaid token", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to exchange token"));
        }
    }

    @GetMapping("/balances")
    public ResponseEntity<?> getBalances(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (bankDataProvider == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Plaid service not available"));
        }
        try {
            User user = requireUser(userPrincipal);
            Map<String, Object> balances = bankDataProvider.fetchBalances(user);
            return ResponseEntity.ok(new BalancesResponse(
                (Long) balances.get("totalAvailableCents"),
                (Instant) balances.get("asOf")
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        } catch (PlaidApiException ex) {
            PlaidError error = ex.error();
            logger.warn("Plaid balances failed: type={} code={} message={}", error.type(), error.code(), error.message());
            return ResponseEntity.status(ex.status())
                .body(Map.of(
                    "code", error.code(),
                    "type", error.type(),
                    "message", error.message()
                ));
        } catch (Exception e) {
            logger.error("Failed to fetch Plaid balances", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to get balances"));
        }
    }

    @PostMapping("/transactions/sync")
    public ResponseEntity<?> syncTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody(required = false) PlaidTransactionsSyncRequest request) {
        if (bankDataProvider == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Plaid service not available"));
        }
        try {
            User user = requireUser(userPrincipal);
            String cursor = request != null ? request.cursor() : null;
            Map<String, Object> result = bankDataProvider.syncTransactions(user, cursor);
            if (cursor != null) {
                result = new HashMap<>(result);
                result.putIfAbsent("receivedCursor", cursor);
            }
            return ResponseEntity.ok(result);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        } catch (PlaidApiException ex) {
            PlaidError error = ex.error();
            logger.warn("Plaid transaction sync failed: type={} code={} message={}", error.type(), error.code(), error.message());
            return ResponseEntity.status(ex.status())
                .body(Map.of(
                    "code", error.code(),
                    "type", error.type(),
                    "message", error.message()
                ));
        } catch (Exception e) {
            logger.error("Failed to sync Plaid transactions", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to sync transactions"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getPlaidStatus(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (plaidItemRepository == null) {
            return ResponseEntity.ok(Map.of("hasItem", false));
        }
        try {
            User user = requireUser(userPrincipal);
            boolean hasItem = !plaidItemRepository.findByUser(user).isEmpty();
            return ResponseEntity.ok(Map.of("hasItem", hasItem));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        }
    }

    @PostMapping("/sync/initial")
    public ResponseEntity<?> runInitialBackfill(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (plaidSyncService == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Plaid sync service unavailable"));
        }
        try {
            User user = requireUser(userPrincipal);
            PlaidSyncService.SyncResult result = plaidSyncService.initialBackfill(user.getId());
            return ResponseEntity.ok(Map.of(
                "accountsUpserted", result.accountsUpserted(),
                "transactionsUpserted", result.transactionsUpserted()
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        } catch (Exception e) {
            logger.error("Failed to run initial Plaid sync", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to run initial sync"));
        }
    }

    @PostMapping("/sync/incremental")
    public ResponseEntity<?> runIncrementalSync(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (plaidSyncService == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Plaid sync service unavailable"));
        }
        try {
            User user = requireUser(userPrincipal);
            PlaidSyncService.SyncResult result = plaidSyncService.incrementalSync(user.getId());
            return ResponseEntity.ok(Map.of(
                "accountsUpserted", result.accountsUpserted(),
                "transactionsUpserted", result.transactionsUpserted()
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        } catch (Exception e) {
            logger.error("Failed to run incremental Plaid sync", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to run incremental sync"));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> webhook) {
        if (bankDataProvider == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Plaid service not available"));
        }
        try {
            String itemId = (String) webhook.get("item_id");
            String webhookType = (String) webhook.get("webhook_type");
            String webhookCode = (String) webhook.get("webhook_code");

            bankDataProvider.handleWebhook(itemId, webhookType, webhookCode);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Failed to process Plaid webhook", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to process webhook"));
        }
    }

    private static class UnauthorizedException extends RuntimeException {}
}
