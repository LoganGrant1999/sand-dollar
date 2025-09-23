package com.sanddollar.repository;

import com.sanddollar.entity.PlaidItem;
import com.sanddollar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaidItemRepository extends JpaRepository<PlaidItem, Long> {
    List<PlaidItem> findByUser(User user);
    Optional<PlaidItem> findByItemId(String itemId);
    List<PlaidItem> findByUserAndStatus(User user, PlaidItem.PlaidItemStatus status);

    // Additional methods for mock data management
    void deleteByUser(User user);
}