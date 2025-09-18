package com.sanddollar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnore
    private Account account;

    @NotBlank
    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @NotBlank
    @Column(name = "plaid_transaction_id", unique = true, nullable = false)
    private String plaidTransactionId;

    @Column(name = "pending_transaction_id")
    private String pendingTransactionId;

    @Column(name = "pending", nullable = false)
    private Boolean pending = false;

    @Column(nullable = false)
    private LocalDate date;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(length = 3)
    private String currency = "USD";

    @Column(name = "category_top", length = 100)
    private String categoryTop;

    @Column(name = "category_sub", length = 100)
    private String categorySub;

    @Column(name = "is_transfer")
    private Boolean isTransfer = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public Transaction() {}

    public Transaction(Account account, String externalId, LocalDate date, String name, 
                      String merchantName, Long amountCents, String categoryTop, String categorySub) {
        this.account = account;
        this.externalId = externalId;
        this.plaidTransactionId = externalId;
        this.date = date;
        this.name = name;
        this.merchantName = merchantName;
        this.amountCents = amountCents;
        this.categoryTop = categoryTop;
        this.categorySub = categorySub;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getPlaidTransactionId() { return plaidTransactionId; }
    public void setPlaidTransactionId(String plaidTransactionId) { this.plaidTransactionId = plaidTransactionId; }

    public String getPendingTransactionId() { return pendingTransactionId; }
    public void setPendingTransactionId(String pendingTransactionId) { this.pendingTransactionId = pendingTransactionId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCategoryTop() { return categoryTop; }
    public void setCategoryTop(String categoryTop) { this.categoryTop = categoryTop; }

    public String getCategorySub() { return categorySub; }
    public void setCategorySub(String categorySub) { this.categorySub = categorySub; }

    public Boolean getIsTransfer() { return isTransfer; }
    public void setIsTransfer(Boolean isTransfer) { this.isTransfer = isTransfer; }

    public Boolean getPending() { return pending; }
    public void setPending(Boolean pending) { this.pending = pending; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
