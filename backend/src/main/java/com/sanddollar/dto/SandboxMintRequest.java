package com.sanddollar.dto;

import java.util.List;

public class SandboxMintRequest {
    private String accountId;
    private Integer count = 20;
    private String startDate;
    private String endDate;
    private Double amountMin = 5.0;
    private Double amountMax = 120.0;
    private List<String> merchantNames = List.of("Taco Llama", "Grocerly", "Rides Co", "CoffeeCat");
    private List<String> category = List.of("Shops", "Groceries");

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public Double getAmountMin() { return amountMin; }
    public void setAmountMin(Double amountMin) { this.amountMin = amountMin; }

    public Double getAmountMax() { return amountMax; }
    public void setAmountMax(Double amountMax) { this.amountMax = amountMax; }

    public List<String> getMerchantNames() { return merchantNames; }
    public void setMerchantNames(List<String> merchantNames) { this.merchantNames = merchantNames; }

    public List<String> getCategory() { return category; }
    public void setCategory(List<String> category) { this.category = category; }
}