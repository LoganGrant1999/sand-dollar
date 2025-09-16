package com.sanddollar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BudgetWizardResponse {
    
    private UUID id;
    private Integer month;
    private Integer year;
    private BigDecimal income;
    
    @JsonProperty("fixed")
    private List<AllocationResponse> fixedAllocations;
    
    @JsonProperty("variable")
    private List<AllocationResponse> variableAllocations;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static class AllocationResponse {
        private UUID id;
        private String name;
        private BigDecimal amount;
        
        public AllocationResponse() {}
        
        public AllocationResponse(UUID id, String name, BigDecimal amount) {
            this.id = id;
            this.name = name;
            this.amount = amount;
        }
        
        // Getters and Setters
        public UUID getId() {
            return id;
        }
        
        public void setId(UUID id) {
            this.id = id;
        }
        
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
    public BudgetWizardResponse() {}
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
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
    
    public List<AllocationResponse> getFixedAllocations() {
        return fixedAllocations;
    }
    
    public void setFixedAllocations(List<AllocationResponse> fixedAllocations) {
        this.fixedAllocations = fixedAllocations;
    }
    
    public List<AllocationResponse> getVariableAllocations() {
        return variableAllocations;
    }
    
    public void setVariableAllocations(List<AllocationResponse> variableAllocations) {
        this.variableAllocations = variableAllocations;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}