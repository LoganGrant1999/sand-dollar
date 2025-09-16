package com.sanddollar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BudgetAdjustmentRequest {
    private String instruction;
    private BudgetScope scope;
    private Boolean confirm;
    private String sourceCategory;

    public static class BudgetScope {
        private Integer month;
        private Integer year;

        public BudgetScope() {}

        public BudgetScope(Integer month, Integer year) {
            this.month = month;
            this.year = year;
        }

        // Getters and setters
        public Integer getMonth() { return month; }
        public void setMonth(Integer month) { this.month = month; }

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
    }

    public BudgetAdjustmentRequest() {}

    public BudgetAdjustmentRequest(String instruction, BudgetScope scope, Boolean confirm, String sourceCategory) {
        this.instruction = instruction;
        this.scope = scope;
        this.confirm = confirm;
        this.sourceCategory = sourceCategory;
    }

    // Getters and setters
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public BudgetScope getScope() { return scope; }
    public void setScope(BudgetScope scope) { this.scope = scope; }

    public Boolean getConfirm() { return confirm; }
    public void setConfirm(Boolean confirm) { this.confirm = confirm; }

    public String getSourceCategory() { return sourceCategory; }
    public void setSourceCategory(String sourceCategory) { this.sourceCategory = sourceCategory; }
}