package com.sanddollar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public class BudgetRequest {
    @NotNull
    @Positive
    @JsonProperty("monthlyIncome")
    private BigDecimal monthlyIncome;

    @NotNull
    @DecimalMin("0.0")
    @JsonProperty("fixedExpenses")
    private BigDecimal fixedExpenses;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @JsonProperty("savingsRate")
    private BigDecimal savingsRate;

    @JsonProperty("categories")
    private List<String> categories;

    // Default constructor for Jackson
    public BudgetRequest() {
    }

    // Constructor with all fields
    public BudgetRequest(BigDecimal monthlyIncome, BigDecimal fixedExpenses, BigDecimal savingsRate, List<String> categories) {
        this.monthlyIncome = monthlyIncome;
        this.fixedExpenses = fixedExpenses;
        this.savingsRate = savingsRate;
        this.categories = categories;
    }

    // Getters and setters
    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getFixedExpenses() {
        return fixedExpenses;
    }

    public void setFixedExpenses(BigDecimal fixedExpenses) {
        this.fixedExpenses = fixedExpenses;
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(BigDecimal savingsRate) {
        this.savingsRate = savingsRate;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}