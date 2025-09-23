package com.sanddollar.budgeting;

import com.sanddollar.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class TransferHeuristics {

    private static final Set<String> TRANSFER_KEYWORDS = Set.of(
        "transfer", "xfer", "online transfer", "external transfer", "internal transfer",
        "account transfer", "mobile transfer", "wire transfer", "ach transfer",
        "withdrawal", "payment to", "payment from"
    );

    private static final Set<String> TRANSFER_CATEGORIES = Set.of(
        "transfer", "bank fees"
    );

    private static final Set<String> CREDIT_CARD_TRANSFER_CATEGORIES = Set.of(
        "Credit Card Payment", "Transfer", "Transfer Out", "Transfer In"
    );

    private static final Pattern PAYMENT_PATTERN = Pattern.compile(
        ".*(?:payment (?:received|thank you|to|posted)|cardmember services|autopay|statement|bill pay|online payment).*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
        ".*(?:account|acct)\\s*(?:#|\\*)?\\d{2,}.*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONFIRMATION_PATTERN = Pattern.compile(
        ".*(?:ref|confirmation|conf)\\s*#?\\s*[a-z0-9]{6,}.*",
        Pattern.CASE_INSENSITIVE
    );

    @Autowired
    @Qualifier("issuerSet")
    private Set<String> issuerSet;

    public boolean isTransfer(Transaction transaction) {
        if (transaction == null) {
            return false;
        }

        // Check category-based transfers (credit card payments, transfers)
        if (isCreditCardOrTransferCategory(transaction)) {
            return true;
        }

        // Check description patterns
        if (hasTransferDescriptionPattern(transaction.getName())) {
            return true;
        }

        // Check merchant name against issuer set
        if (isFromCardIssuer(transaction.getMerchantName())) {
            return true;
        }

        // Legacy transfer detection
        return isLegacyTransfer(transaction.getName(),
                               transaction.getCategoryTop(),
                               transaction.getCategorySub());
    }

    public boolean isTransfer(String transactionName, String categoryTop, String categorySub) {
        // Legacy method for backward compatibility
        return isLegacyTransfer(transactionName, categoryTop, categorySub);
    }

    private boolean isCreditCardOrTransferCategory(Transaction transaction) {
        String categorySub = transaction.getCategorySub();
        String categoryTop = transaction.getCategoryTop();

        // Check if category sub contains any of the transfer keywords (partial match)
        if (categorySub != null) {
            String lowerSub = categorySub.toLowerCase();
            if (lowerSub.contains("credit card payment") ||
                lowerSub.contains("transfer in") ||
                lowerSub.contains("transfer out") ||
                lowerSub.contains("account transfer") ||
                lowerSub.contains("savings") && (lowerSub.contains("transfer")) ||
                lowerSub.contains("withdrawal") ||
                lowerSub.contains("loan payments")) {
                return true;
            }
        }

        return (categorySub != null && CREDIT_CARD_TRANSFER_CATEGORIES.contains(categorySub)) ||
               (categoryTop != null && categoryTop.toLowerCase().startsWith("transfer"));
    }

    private boolean hasTransferDescriptionPattern(String description) {
        if (description == null) {
            return false;
        }

        return PAYMENT_PATTERN.matcher(description).matches();
    }

    private boolean isFromCardIssuer(String merchantName) {
        if (merchantName == null) {
            return false;
        }

        String normalizedMerchant = merchantName.toLowerCase().trim();
        return issuerSet.stream()
            .anyMatch(issuer -> normalizedMerchant.contains(issuer.toLowerCase()));
    }

    private boolean isLegacyTransfer(String transactionName, String categoryTop, String categorySub) {
        if (transactionName == null) {
            return false;
        }

        String name = transactionName.toLowerCase().trim();
        String topCategory = categoryTop != null ? categoryTop.toLowerCase().trim() : "";
        String subCategory = categorySub != null ? categorySub.toLowerCase().trim() : "";

        return containsTransferKeywords(name) ||
               hasTransferCategory(topCategory, subCategory) ||
               hasAccountPattern(name) ||
               hasConfirmationPattern(name);
    }

    private boolean containsTransferKeywords(String name) {
        return TRANSFER_KEYWORDS.stream()
            .anyMatch(keyword -> name.contains(keyword));
    }

    private boolean hasTransferCategory(String topCategory, String subCategory) {
        return TRANSFER_CATEGORIES.contains(topCategory) ||
               TRANSFER_CATEGORIES.contains(subCategory);
    }

    private boolean hasAccountPattern(String name) {
        return ACCOUNT_PATTERN.matcher(name).matches();
    }

    private boolean hasConfirmationPattern(String name) {
        return CONFIRMATION_PATTERN.matcher(name).matches();
    }
}