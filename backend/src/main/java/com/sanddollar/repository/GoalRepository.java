package com.sanddollar.repository;

import com.sanddollar.entity.Goal;
import com.sanddollar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    List<Goal> findByUser(User user);

    List<Goal> findByUserAndStatus(User user, Goal.GoalStatus status);

    List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Goal.GoalStatus status);
}