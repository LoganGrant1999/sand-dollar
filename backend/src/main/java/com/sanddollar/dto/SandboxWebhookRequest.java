package com.sanddollar.dto;

public class SandboxWebhookRequest {
    private String itemId;
    private String webhookCode = "SYNC_UPDATES_AVAILABLE";

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getWebhookCode() { return webhookCode; }
    public void setWebhookCode(String webhookCode) { this.webhookCode = webhookCode; }
}