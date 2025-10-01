package com.sanddollar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "goals")
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 32)
    private GoalType goalType;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @NotNull
    @Positive
    @Column(name = "target_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;

    @NotNull
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "plan_monthly_contribution", precision = 12, scale = 2)
    private BigDecimal planMonthlyContribution;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private GoalStatus status = GoalStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trip_metadata", columnDefinition = "jsonb")
    private Map<String, Object> tripMetadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "nudge_suggested")
    private Boolean nudgeSuggested = false;

    @Column(name = "nudge_amount", precision = 12, scale = 2)
    private BigDecimal nudgeAmount;

    // Constructors
    public Goal() {}

    public Goal(User user, GoalType goalType, String name, BigDecimal targetAmount, LocalDate targetDate) {
        this.user = user;
        this.goalType = goalType;
        this.name = name;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public BigDecimal getPlanMonthlyContribution() { return planMonthlyContribution; }
    public void setPlanMonthlyContribution(BigDecimal planMonthlyContribution) { this.planMonthlyContribution = planMonthlyContribution; }

    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }

    public Map<String, Object> getTripMetadata() { return tripMetadata; }
    public void setTripMetadata(Map<String, Object> tripMetadata) { this.tripMetadata = tripMetadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getNudgeSuggested() { return nudgeSuggested; }
    public void setNudgeSuggested(Boolean nudgeSuggested) { this.nudgeSuggested = nudgeSuggested; }

    public BigDecimal getNudgeAmount() { return nudgeAmount; }
    public void setNudgeAmount(BigDecimal nudgeAmount) { this.nudgeAmount = nudgeAmount; }

    public enum GoalType {
        TRIP, PURCHASE
    }

    public enum GoalStatus {
        ACTIVE, COMPLETED, PAUSED, CANCELLED
    }

    // Backward compatibility methods for existing services
    @Deprecated
    public Long getTargetCents() {
        return targetAmount != null ? targetAmount.multiply(new java.math.BigDecimal(100)).longValue() : 0L;
    }

    @Deprecated
    public void setTargetCents(Long targetCents) {
        this.targetAmount = targetCents != null ? new java.math.BigDecimal(targetCents).divide(new java.math.BigDecimal(100)) : null;
    }

    @Deprecated
    public Long getSavedCents() {
        // For backward compatibility, we'll compute this from contributions
        // In practice, this should use the GoalService.computeSavedAmount method
        return 0L; // Placeholder - services should use GoalService.computeSavedAmount
    }

    @Deprecated
    public void setSavedCents(Long savedCents) {
        // This is deprecated - contributions should be managed through GoalContribution entities
    }
}