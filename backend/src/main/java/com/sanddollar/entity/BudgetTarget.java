package com.sanddollar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

@Entity
@Table(
    name = "budget_targets",
    uniqueConstraints = @UniqueConstraint(
        name = "idx_budget_targets_user_month_category",
        columnNames = {"user_id", "month", "category"}
    )
)
public class BudgetTarget {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @NotBlank
    @Column(name = "month", nullable = false, length = 7)
    private String month;
    
    @NotBlank
    @Column(name = "category", nullable = false)
    private String category;
    
    @NotNull
    @Positive
    @Column(name = "target_cents", nullable = false)
    private Integer targetCents;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    public BudgetTarget() {}
    
    public BudgetTarget(Long userId, String month, String category, Integer targetCents, String reason) {
        this.userId = userId;
        this.month = month;
        this.category = category;
        this.targetCents = targetCents;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getTargetCents() { return targetCents; }
    public void setTargetCents(Integer targetCents) { this.targetCents = targetCents; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}