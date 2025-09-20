package com.sanddollar.budgeting;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class RefundHeuristics {

    private static final Set<String> REFUND_KEYWORDS = Set.of(
        "refund", "return", "reversal", "credit", "adjustment", "chargeback",
        "dispute", "cashback", "cash back", "rebate", "reimbursement"
    );

    private static final Pattern REFUND_PATTERN = Pattern.compile(
        ".*(?:return|refund|credit|reversal|chargeback).*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MERCHANT_RETURN_PATTERN = Pattern.compile(
        ".*(?:return|refund)\\s+(?:from|to)\\s+.*",
        Pattern.CASE_INSENSITIVE
    );

    public boolean isRefund(String transactionName, long amountCents) {
        if (transactionName == null) {
            return false;
        }

        String name = transactionName.toLowerCase().trim();

        return (amountCents > 0) && (
            containsRefundKeywords(name) ||
            matchesRefundPattern(name) ||
            matchesMerchantReturnPattern(name)
        );
    }

    private boolean containsRefundKeywords(String name) {
        return REFUND_KEYWORDS.stream()
            .anyMatch(keyword -> name.contains(keyword));
    }

    private boolean matchesRefundPattern(String name) {
        return REFUND_PATTERN.matcher(name).matches();
    }

    private boolean matchesMerchantReturnPattern(String name) {
        return MERCHANT_RETURN_PATTERN.matcher(name).matches();
    }
}