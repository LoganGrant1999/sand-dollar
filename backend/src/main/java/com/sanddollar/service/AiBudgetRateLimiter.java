package com.sanddollar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiBudgetRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Map<Long, Deque<Instant>> requests = new ConcurrentHashMap<>();

    public AiBudgetRateLimiter() {
        this(5, Duration.ofMinutes(1));
    }

    public AiBudgetRateLimiter(@Value("${feature.ai-budget.rate-limit-per-minute:5}") int maxRequestsPerMinute) {
        this(maxRequestsPerMinute, Duration.ofMinutes(1));
    }

    public AiBudgetRateLimiter(int maxRequestsPerMinute, Duration window) {
        this.maxRequests = Math.max(1, maxRequestsPerMinute);
        this.window = window;
    }

    public boolean tryConsume(long userId) {
        Instant now = Instant.now();
        Deque<Instant> deque = requests.computeIfAbsent(userId, key -> new ArrayDeque<>());

        // drop expired entries
        Instant expiration = now.minus(window);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(expiration)) {
            deque.pollFirst();
        }

        if (deque.size() >= maxRequests) {
            return false;
        }

        deque.addLast(now);
        return true;
    }

    public void clear(long userId) {
        requests.remove(userId);
    }
}
