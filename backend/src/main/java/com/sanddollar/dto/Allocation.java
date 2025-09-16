package com.sanddollar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class Allocation {
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("pctOfIncome")
    private BigDecimal pctOfIncome;

    public Allocation() {
    }

    public Allocation(String category, BigDecimal amount, BigDecimal pctOfIncome) {
        this.category = category;
        this.amount = amount;
        this.pctOfIncome = pctOfIncome;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPctOfIncome() {
        return pctOfIncome;
    }

    public void setPctOfIncome(BigDecimal pctOfIncome) {
        this.pctOfIncome = pctOfIncome;
    }
}