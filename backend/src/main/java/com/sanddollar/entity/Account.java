package com.sanddollar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plaid_item_id", nullable = false)
    @JsonIgnore
    private PlaidItem plaidItem;

    @NotBlank
    @Column(name = "account_id", unique = true, nullable = false)
    private String accountId;

    @NotBlank
    @Column(name = "plaid_account_id", unique = true, nullable = false)
    private String plaidAccountId;

    @Column(length = 10)
    private String mask;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "institution_name")
    private String institutionName;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 50)
    private String subtype;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BalanceSnapshot> balanceSnapshots;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Transaction> transactions;

    // Constructors
    public Account() {}

    public Account(User user, PlaidItem plaidItem, String accountId, String mask, String name, 
                   String institutionName, String type, String subtype) {
        this.user = user;
        this.plaidItem = plaidItem;
        this.accountId = accountId;
        this.plaidAccountId = accountId;
        this.mask = mask;
        this.name = name;
        this.institutionName = institutionName;
        this.type = type;
        this.subtype = subtype;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public PlaidItem getPlaidItem() { return plaidItem; }
    public void setPlaidItem(PlaidItem plaidItem) { this.plaidItem = plaidItem; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getPlaidAccountId() { return plaidAccountId; }
    public void setPlaidAccountId(String plaidAccountId) { this.plaidAccountId = plaidAccountId; }

    public String getMask() { return mask; }
    public void setMask(String mask) { this.mask = mask; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubtype() { return subtype; }
    public void setSubtype(String subtype) { this.subtype = subtype; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<BalanceSnapshot> getBalanceSnapshots() { return balanceSnapshots; }
    public void setBalanceSnapshots(List<BalanceSnapshot> balanceSnapshots) { this.balanceSnapshots = balanceSnapshots; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}
