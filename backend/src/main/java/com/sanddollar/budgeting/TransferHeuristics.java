package com.sanddollar.budgeting;

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

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
        ".*(?:account|acct)\\s*(?:#|\\*)?\\d{2,}.*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONFIRMATION_PATTERN = Pattern.compile(
        ".*(?:ref|confirmation|conf)\\s*#?\\s*[a-z0-9]{6,}.*",
        Pattern.CASE_INSENSITIVE
    );

    public boolean isTransfer(String transactionName, String categoryTop, String categorySub) {
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