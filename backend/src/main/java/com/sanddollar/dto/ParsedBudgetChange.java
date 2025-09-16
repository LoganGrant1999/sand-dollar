package com.sanddollar.dto;

import java.math.BigDecimal;
import java.util.List;

public class ParsedBudgetChange {
    private List<CategoryChange> changes;
    private String sourceCategory;

    public static class CategoryChange {
        private String category;
        private BigDecimal deltaAmount; // null if using percentage
        private BigDecimal deltaPercent; // null if using amount

        public CategoryChange() {}

        public CategoryChange(String category, BigDecimal deltaAmount, BigDecimal deltaPercent) {
            this.category = category;
            this.deltaAmount = deltaAmount;
            this.deltaPercent = deltaPercent;
        }

        // Getters and setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public BigDecimal getDeltaAmount() { return deltaAmount; }
        public void setDeltaAmount(BigDecimal deltaAmount) { this.deltaAmount = deltaAmount; }

        public BigDecimal getDeltaPercent() { return deltaPercent; }
        public void setDeltaPercent(BigDecimal deltaPercent) { this.deltaPercent = deltaPercent; }
    }

    public ParsedBudgetChange() {}

    public ParsedBudgetChange(List<CategoryChange> changes, String sourceCategory) {
        this.changes = changes;
        this.sourceCategory = sourceCategory;
    }

    // Getters and setters
    public List<CategoryChange> getChanges() { return changes; }
    public void setChanges(List<CategoryChange> changes) { this.changes = changes; }

    public String getSourceCategory() { return sourceCategory; }
    public void setSourceCategory(String sourceCategory) { this.sourceCategory = sourceCategory; }
}