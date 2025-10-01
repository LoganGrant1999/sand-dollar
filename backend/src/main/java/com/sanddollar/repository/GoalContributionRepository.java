package com.sanddollar.repository;

import com.sanddollar.entity.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    List<GoalContribution> findByGoalIdOrderByCreatedAtAsc(Long goalId);

    List<GoalContribution> findByGoalIdOrderByCreatedAtDesc(Long goalId);
}