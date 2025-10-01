package com.sanddollar.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sanddollar.service.GoalNudgeService;
import com.sanddollar.service.PlaidSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plaid/webhook")
@Profile("plaid")
public class PlaidWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(PlaidWebhookController.class);

    @Autowired
    private PlaidSyncService plaidSyncService;

    @Autowired
    private GoalNudgeService goalNudgeService;

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody PlaidWebhookPayload payload) {
        logger.info("Received Plaid webhook: {} for item {}", payload.webhookType(), payload.itemId());

        try {
            switch (payload.webhookType()) {
                case "TRANSACTIONS":
                    handleTransactionsWebhook(payload);
                    break;
                case "ITEM":
                    handleItemWebhook(payload);
                    break;
                default:
                    logger.info("Unhandled webhook type: {}", payload.webhookType());
            }

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleTransactionsWebhook(PlaidWebhookPayload payload) {
        String webhookCode = payload.webhookCode();
        logger.info("Processing TRANSACTIONS webhook: {}", webhookCode);

        switch (webhookCode) {
            case "SYNC_UPDATES_AVAILABLE":
            case "DEFAULT_UPDATE":
                // Sync transactions for all users with this item
                syncTransactionsForItem(payload.itemId());
                break;
            case "INITIAL_UPDATE":
                logger.info("Initial transaction update for item: {}", payload.itemId());
                syncTransactionsForItem(payload.itemId());
                break;
            default:
                logger.info("Unhandled TRANSACTIONS webhook code: {}", webhookCode);
        }
    }

    private void handleItemWebhook(PlaidWebhookPayload payload) {
        String webhookCode = payload.webhookCode();
        logger.info("Processing ITEM webhook: {}", webhookCode);

        switch (webhookCode) {
            case "ERROR":
                logger.warn("Item error for {}: {}", payload.itemId(), payload.error());
                break;
            case "PENDING_EXPIRATION":
                logger.warn("Item pending expiration: {}", payload.itemId());
                break;
            default:
                logger.info("Unhandled ITEM webhook code: {}", webhookCode);
        }
    }

    private void syncTransactionsForItem(String itemId) {
        try {
            // Find users with this Plaid item and sync their transactions
            List<Long> userIds = plaidSyncService.getUserIdsByItemId(itemId);

            for (Long userId : userIds) {
                logger.info("Syncing transactions for user {} due to webhook", userId);
                plaidSyncService.incrementalSync(userId);

                // Check for nudge opportunities after sync
                goalNudgeService.checkForNudgeOpportunities(userId);
            }
        } catch (Exception e) {
            logger.error("Error syncing transactions for item: " + itemId, e);
        }
    }

    public record PlaidWebhookPayload(
        @JsonProperty("webhook_type") String webhookType,
        @JsonProperty("webhook_code") String webhookCode,
        @JsonProperty("item_id") String itemId,
        @JsonProperty("new_transactions") Integer newTransactions,
        @JsonProperty("removed_transactions") List<String> removedTransactions,
        @JsonProperty("error") Map<String, Object> error
    ) {}
}