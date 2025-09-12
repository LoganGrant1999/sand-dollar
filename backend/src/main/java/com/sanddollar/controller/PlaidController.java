package com.sanddollar.controller;

import com.sanddollar.dto.BalancesResponse;
import com.sanddollar.dto.PlaidExchangeRequest;
import com.sanddollar.entity.PlaidItem;
import com.sanddollar.entity.User;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.BankDataProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/plaid")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class PlaidController {

    @Autowired
    private BankDataProvider bankDataProvider;

    @PostMapping("/link-token")
    public ResponseEntity<?> createLinkToken(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            String linkToken = bankDataProvider.createLinkToken(user);
            
            Map<String, String> response = new HashMap<>();
            response.put("link_token", linkToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to create link token: " + e.getMessage()));
        }
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangePublicToken(
            @Valid @RequestBody PlaidExchangeRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            PlaidItem plaidItem = bankDataProvider.exchangePublicToken(user, request.publicToken());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "item_id", plaidItem.getItemId(),
                "institution_name", plaidItem.getInstitutionName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to exchange token: " + e.getMessage()));
        }
    }

    @GetMapping("/balances")
    public ResponseEntity<?> getBalances(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            Map<String, Object> balances = bankDataProvider.fetchBalances(user);
            
            return ResponseEntity.ok(new BalancesResponse(
                (Long) balances.get("totalAvailableCents"),
                (Instant) balances.get("asOf")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get balances: " + e.getMessage()));
        }
    }

    @PostMapping("/transactions/sync")
    public ResponseEntity<?> syncTransactions(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            Map<String, Object> result = bankDataProvider.syncTransactions(user);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to sync transactions: " + e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> webhook) {
        try {
            String itemId = (String) webhook.get("item_id");
            String webhookType = (String) webhook.get("webhook_type");
            String webhookCode = (String) webhook.get("webhook_code");
            
            bankDataProvider.handleWebhook(itemId, webhookType, webhookCode);
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to process webhook: " + e.getMessage()));
        }
    }
}