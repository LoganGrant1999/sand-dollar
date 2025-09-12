package com.sanddollar.service;

import com.sanddollar.entity.User;
import com.sanddollar.entity.PlaidItem;

import java.util.Map;

/**
 * Interface for bank data providers (Plaid, Mock, etc.)
 * Abstracts financial data operations regardless of the underlying provider
 */
public interface BankDataProvider {
    
    /**
     * Create a link token for connecting bank accounts
     * @param user The user requesting the link token
     * @return Link token string
     */
    String createLinkToken(User user);
    
    /**
     * Exchange a public token for an access token and create the connection
     * @param user The user exchanging the token
     * @param publicToken The public token to exchange
     * @return Created PlaidItem (or equivalent connection)
     */
    PlaidItem exchangePublicToken(User user, String publicToken);
    
    /**
     * Synchronize transactions for a user
     * @param user The user to sync transactions for
     * @return Summary of sync results
     */
    Map<String, Object> syncTransactions(User user);
    
    /**
     * Fetch and refresh account balances
     * @param user The user to fetch balances for
     * @return Summary of balance information
     */
    Map<String, Object> fetchBalances(User user);
    
    /**
     * Handle incoming webhooks from the provider
     * @param itemId The item ID from the webhook
     * @param webhookType The type of webhook
     * @param webhookCode The specific webhook code
     */
    void handleWebhook(String itemId, String webhookType, String webhookCode);
}