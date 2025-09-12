package com.sanddollar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "budget_lines")
public class BudgetLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private BudgetPeriod period;

    @NotBlank
    @Column(name = "category_top", nullable = false, length = 100)
    private String categoryTop;

    @Positive
    @Column(name = "limit_cents", nullable = false)
    private Long limitCents;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public BudgetLine() {}

    public BudgetLine(BudgetPeriod period, String categoryTop, Long limitCents) {
        this.period = period;
        this.categoryTop = categoryTop;
        this.limitCents = limitCents;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { this.period = period; }

    public String getCategoryTop() { return categoryTop; }
    public void setCategoryTop(String categoryTop) { this.categoryTop = categoryTop; }

    public Long getLimitCents() { return limitCents; }
    public void setLimitCents(Long limitCents) { this.limitCents = limitCents; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}