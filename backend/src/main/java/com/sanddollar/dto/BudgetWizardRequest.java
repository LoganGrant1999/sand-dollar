package com.sanddollar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class BudgetWizardRequest {
    
    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;
    
    @NotNull
    @Min(2020)
    @Max(2030)
    private Integer year;
    
    @NotNull
    @JsonProperty("income")
    private BigDecimal income;
    
    @Valid
    @NotEmpty
    private List<AllocationItem> fixed;
    
    @Valid 
    @NotEmpty
    private List<AllocationItem> variable;
    
    public static class AllocationItem {
        @NotNull
        private String name;
        
        @NotNull
        private BigDecimal amount;
        
        // Constructor
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
    public BudgetWizardRequest() {}
    
    // Getters and Setters
    public Integer getMonth() {
        return month;
    }
    
    public void setMonth(Integer month) {
        this.month = month;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public BigDecimal getIncome() {
        return income;
    }
    
    public void setIncome(BigDecimal income) {
        this.income = income;
    }
    
    public List<AllocationItem> getFixed() {
        return fixed;
    }
    
    public void setFixed(List<AllocationItem> fixed) {
        this.fixed = fixed;
    }
    
    public List<AllocationItem> getVariable() {
        return variable;
    }
    
    public void setVariable(List<AllocationItem> variable) {
        this.variable = variable;
    }
}