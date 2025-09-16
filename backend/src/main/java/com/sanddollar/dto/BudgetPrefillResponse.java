package com.sanddollar.dto;

import java.math.BigDecimal;
import java.util.List;

public class BudgetPrefillResponse {
    
    private BigDecimal incomeEstimate;
    private List<AllocationItem> fixed;
    private List<AllocationItem> variableSuggestions;
    
    public static class AllocationItem {
        private String name;
        private BigDecimal amount;
        
        public AllocationItem() {}
        
        public AllocationItem(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
    
    // Default constructor
    public BudgetPrefillResponse() {}
    
    // Constructor
    public BudgetPrefillResponse(BigDecimal incomeEstimate, List<AllocationItem> fixed, List<AllocationItem> variableSuggestions) {
        this.incomeEstimate = incomeEstimate;
        this.fixed = fixed;
        this.variableSuggestions = variableSuggestions;
    }
    
    // Getters and Setters
    public BigDecimal getIncomeEstimate() {
        return incomeEstimate;
    }
    
    public void setIncomeEstimate(BigDecimal incomeEstimate) {
        this.incomeEstimate = incomeEstimate;
    }
    
    public List<AllocationItem> getFixed() {
        return fixed;
    }
    
    public void setFixed(List<AllocationItem> fixed) {
        this.fixed = fixed;
    }
    
    public List<AllocationItem> getVariableSuggestions() {
        return variableSuggestions;
    }
    
    public void setVariableSuggestions(List<AllocationItem> variableSuggestions) {
        this.variableSuggestions = variableSuggestions;
    }
}