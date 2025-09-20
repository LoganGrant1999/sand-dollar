package com.sanddollar.budgeting;

import com.sanddollar.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class IncomeDetector {

    @Autowired
    @Qualifier("incomeWhitelistNames")
    private Set<String> incomeWhitelistNames;

    @Autowired
    @Qualifier("incomeWhitelistCategories")
    private Set<String> incomeWhitelistCategories;

    @Autowired
    private RefundHeuristics refundHeuristics;

    @Autowired
    private TransferHeuristics transferHeuristics;

    public boolean isBaselineIncome(Transaction transaction) {
        if (transaction.getAmountCents() <= 0) {
            return false;
        }

        if (refundHeuristics.isRefund(transaction.getName(), transaction.getAmountCents()) ||
            transferHeuristics.isTransfer(transaction)) {
            return false;
        }

        String normalizedName = normalizeTransactionName(transaction.getName());
        String categorySub = transaction.getCategorySub();

        return incomeWhitelistNames.stream()
                .anyMatch(name -> normalizedName.contains(name.toLowerCase())) ||
               (categorySub != null && incomeWhitelistCategories.contains(categorySub));
    }

    public boolean isOtherInflow(Transaction transaction) {
        if (transaction.getAmountCents() <= 0) {
            return false;
        }

        if (refundHeuristics.isRefund(transaction.getName(), transaction.getAmountCents()) ||
            transferHeuristics.isTransfer(transaction)) {
            return false;
        }

        return !isBaselineIncome(transaction);
    }

    private String normalizeTransactionName(String name) {
        return name != null ? name.toLowerCase().trim() : "";
    }
}