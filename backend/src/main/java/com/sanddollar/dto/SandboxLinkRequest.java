package com.sanddollar.dto;

import java.util.List;

public class SandboxLinkRequest {
    private String institutionId = "ins_109508";
    private List<String> products = List.of("transactions");
    private String startDate;
    private String endDate;
    private Boolean createWebhook = true;

    public String getInstitutionId() { return institutionId; }
    public void setInstitutionId(String institutionId) { this.institutionId = institutionId; }

    public List<String> getProducts() { return products; }
    public void setProducts(List<String> products) { this.products = products; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public Boolean getCreateWebhook() { return createWebhook; }
    public void setCreateWebhook(Boolean createWebhook) { this.createWebhook = createWebhook; }
}