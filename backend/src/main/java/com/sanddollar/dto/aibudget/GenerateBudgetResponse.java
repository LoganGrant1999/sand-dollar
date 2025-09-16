package com.sanddollar.dto.aibudget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.util.List;

public class GenerateBudgetResponse {
    
    @NotBlank(message = "Month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in YYYY-MM format")
    private String month;
    
    @NotNull(message = "Targets by category is required")
    @Valid
    private List<CategoryTarget> targetsByCategory;
    
    @NotNull(message = "Summary is required")
    @Valid
    private BudgetSummary summary;
    
    @PositiveOrZero(message = "Prompt tokens must be positive or zero")
    private Integer promptTokens;
    
    @PositiveOrZero(message = "Completion tokens must be positive or zero")
    private Integer completionTokens;
    
    public GenerateBudgetResponse() {}
    
    public GenerateBudgetResponse(String month, List<CategoryTarget> targetsByCategory, BudgetSummary summary, Integer promptTokens, Integer completionTokens) {
        this.month = month;
        this.targetsByCategory = targetsByCategory;
        this.summary = summary;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }
    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    
    public List<CategoryTarget> getTargetsByCategory() { return targetsByCategory; }
    public void setTargetsByCategory(List<CategoryTarget> targetsByCategory) { this.targetsByCategory = targetsByCategory; }
    
    public BudgetSummary getSummary() { return summary; }
    public void setSummary(BudgetSummary summary) { this.summary = summary; }
    
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    
    public static class CategoryTarget {
        @NotBlank(message = "Category name is required")
        private String category;
        
        @NotNull(message = "Target amount is required")
        @PositiveOrZero(message = "Target amount must be positive or zero")
        private BigDecimal target;
        
        @NotBlank(message = "Reason is required")
        private String reason;
        
        public CategoryTarget() {}
        
        public CategoryTarget(String category, BigDecimal target, String reason) {
            this.category = category;
            this.target = target;
            this.reason = reason;
        }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public BigDecimal getTarget() { return target; }
        public void setTarget(BigDecimal target) { this.target = target; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    public static class BudgetSummary {
        @NotNull(message = "Savings rate is required")
        @DecimalMin(value = "0.0", message = "Savings rate must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "Savings rate must be between 0 and 1")
        private BigDecimal savingsRate;
        
        private List<String> notes;
        
        public BudgetSummary() {}
        
        public BudgetSummary(BigDecimal savingsRate, List<String> notes) {
            this.savingsRate = savingsRate;
            this.notes = notes;
        }
        
        public BigDecimal getSavingsRate() { return savingsRate; }
        public void setSavingsRate(BigDecimal savingsRate) { this.savingsRate = savingsRate; }
        
        public List<String> getNotes() { return notes; }
        public void setNotes(List<String> notes) { this.notes = notes; }
    }
}