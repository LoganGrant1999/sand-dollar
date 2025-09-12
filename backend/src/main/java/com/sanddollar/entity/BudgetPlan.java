package com.sanddollar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "budget_plans")
public class BudgetPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetPeriodType period;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotBlank
    @Column(name = "plan_json", nullable = false, columnDefinition = "TEXT")
    private String planJson;

    @Enumerated(EnumType.STRING)
    private BudgetStatus status = BudgetStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetPeriod> budgetPeriods;

    // Constructors
    public BudgetPlan() {}

    public BudgetPlan(User user, BudgetPeriodType period, LocalDate startDate, LocalDate endDate, String planJson) {
        this.user = user;
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.planJson = planJson;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BudgetPeriodType getPeriod() { return period; }
    public void setPeriod(BudgetPeriodType period) { this.period = period; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getPlanJson() { return planJson; }
    public void setPlanJson(String planJson) { this.planJson = planJson; }

    public BudgetStatus getStatus() { return status; }
    public void setStatus(BudgetStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<BudgetPeriod> getBudgetPeriods() { return budgetPeriods; }
    public void setBudgetPeriods(List<BudgetPeriod> budgetPeriods) { this.budgetPeriods = budgetPeriods; }

    public enum BudgetPeriodType {
        WEEKLY, BIWEEKLY, MONTHLY
    }

    public enum BudgetStatus {
        ACTIVE, COMPLETED, CANCELLED
    }
}