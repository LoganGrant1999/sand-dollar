package com.sanddollar.dto;

import java.math.BigDecimal;
import java.util.List;

public class BudgetAdjustmentResponse {
    private String status; // "success", "needs_confirmation", "error"
    private String message; // Error message when status is "error"
    private BudgetAdjustmentProposal proposal;
    private List<SourceCategoryOption> options;
    private Object updatedBudget; // Full budget data if successful

    public static class BudgetAdjustmentProposal {
        private List<BudgetDiff> diffs;

        public BudgetAdjustmentProposal() {}

        public BudgetAdjustmentProposal(List<BudgetDiff> diffs) {
            this.diffs = diffs;
        }

        public List<BudgetDiff> getDiffs() { return diffs; }
        public void setDiffs(List<BudgetDiff> diffs) { this.diffs = diffs; }
    }

    public static class BudgetDiff {
        private String category;
        private BigDecimal currentAmount;
        private BigDecimal newAmount;
        private BigDecimal deltaAmount;

        public BudgetDiff() {}

        public BudgetDiff(String category, BigDecimal currentAmount, BigDecimal newAmount, BigDecimal deltaAmount) {
            this.category = category;
            this.currentAmount = currentAmount;
            this.newAmount = newAmount;
            this.deltaAmount = deltaAmount;
        }

        // Getters and setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public BigDecimal getCurrentAmount() { return currentAmount; }
        public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

        public BigDecimal getNewAmount() { return newAmount; }
        public void setNewAmount(BigDecimal newAmount) { this.newAmount = newAmount; }

        public BigDecimal getDeltaAmount() { return deltaAmount; }
        public void setDeltaAmount(BigDecimal deltaAmount) { this.deltaAmount = deltaAmount; }
    }

    public static class SourceCategoryOption {
        private String category;
        private BigDecimal currentAmount;
        private BigDecimal suggestedReduction;

        public SourceCategoryOption() {}

        public SourceCategoryOption(String category, BigDecimal currentAmount, BigDecimal suggestedReduction) {
            this.category = category;
            this.currentAmount = currentAmount;
            this.suggestedReduction = suggestedReduction;
        }

        // Getters and setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public BigDecimal getCurrentAmount() { return currentAmount; }
        public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

        public BigDecimal getSuggestedReduction() { return suggestedReduction; }
        public void setSuggestedReduction(BigDecimal suggestedReduction) { this.suggestedReduction = suggestedReduction; }
    }

    public BudgetAdjustmentResponse() {}

    public BudgetAdjustmentResponse(String status) {
        this.status = status;
    }

    public BudgetAdjustmentResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public BudgetAdjustmentProposal getProposal() { return proposal; }
    public void setProposal(BudgetAdjustmentProposal proposal) { this.proposal = proposal; }

    public List<SourceCategoryOption> getOptions() { return options; }
    public void setOptions(List<SourceCategoryOption> options) { this.options = options; }

    public Object getUpdatedBudget() { return updatedBudget; }
    public void setUpdatedBudget(Object updatedBudget) { this.updatedBudget = updatedBudget; }
}