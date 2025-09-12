package com.sanddollar.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

@Entity
@Table(name = "balance_snapshots")
public class BalanceSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnore
    private Account account;

    @Column(name = "available_cents")
    private Long availableCents;

    @Column(name = "current_cents", nullable = false)
    private Long currentCents;

    @Column(length = 3)
    private String currency = "USD";

    @CreationTimestamp
    @Column(name = "as_of", nullable = false)
    private Instant asOf;

    // Constructors
    public BalanceSnapshot() {}

    public BalanceSnapshot(Account account, Long availableCents, Long currentCents, String currency) {
        this.account = account;
        this.availableCents = availableCents;
        this.currentCents = currentCents;
        this.currency = currency;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Long getAvailableCents() { return availableCents; }
    public void setAvailableCents(Long availableCents) { this.availableCents = availableCents; }

    public Long getCurrentCents() { return currentCents; }
    public void setCurrentCents(Long currentCents) { this.currentCents = currentCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getAsOf() { return asOf; }
    public void setAsOf(Instant asOf) { this.asOf = asOf; }
}