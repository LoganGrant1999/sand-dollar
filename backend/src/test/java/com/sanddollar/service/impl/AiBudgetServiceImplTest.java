package com.sanddollar.service.impl;

import com.sanddollar.dto.aibudget.AcceptBudgetRequest;
import com.sanddollar.dto.aibudget.GenerateBudgetRequest;
import com.sanddollar.dto.aibudget.GenerateBudgetResponse;
import com.sanddollar.dto.aibudget.FinancialSnapshotResponse;
import com.sanddollar.entity.BudgetTarget;
import com.sanddollar.entity.User;
import com.sanddollar.repository.BalanceSnapshotRepository;
import com.sanddollar.repository.BudgetTargetRepository;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.AiBudgetRateLimiter;
import com.sanddollar.service.OpenAiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiBudgetServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private BalanceSnapshotRepository balanceSnapshotRepository;
    @Mock private BudgetTargetRepository budgetTargetRepository;
    @Mock private OpenAiClient openAiClient;

    @InjectMocks
    private AiBudgetServiceImpl aiBudgetService;

    private User currentUser;

    @BeforeEach
    void setUpSecurityContext() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("local@demo");
        currentUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvxyz1234567890abcdefghi1234567890");

        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new UserPrincipal(currentUser), null, List.of())
        );

        ReflectionTestUtils.setField(aiBudgetService, "aiBudgetEnabled", true);
        ReflectionTestUtils.setField(aiBudgetService, "rateLimiter", new AiBudgetRateLimiter(10));
    }

    @Test
    void getFinancialSnapshot_includesTargets() {
        when(transactionRepository.getSpendingByCategory(eq(currentUser), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(
                new Object[]{"Dining", 28000L},
                new Object[]{"Rent", 150000L}
            ));

        when(budgetTargetRepository.findByUserIdAndMonthOrderByCategory(eq(currentUser.getId()), anyString()))
            .thenReturn(List.of(
                new BudgetTarget(currentUser.getId(), "2025-09", "Dining", 30000, "Cap dining"),
                new BudgetTarget(currentUser.getId(), "2025-09", "Rent", 150000, "Monthly rent")
            ));

        FinancialSnapshotResponse snapshot = aiBudgetService.getFinancialSnapshot();

        assertEquals(2, snapshot.getTargetsByCategory().size());
        FinancialSnapshotResponse.CategoryActual dining = snapshot.getActualsByCategory().stream()
            .filter(entry -> entry.getCategory().equals("Dining"))
            .findFirst()
            .orElseThrow();

        assertEquals(new BigDecimal("280.00"), dining.getActual());
        assertEquals(new BigDecimal("300.00"), dining.getTarget());
    }

    @Test
    void generateBudget_enforcesRateLimit() {
        ReflectionTestUtils.setField(aiBudgetService, "rateLimiter", new AiBudgetRateLimiter(1));
        when(transactionRepository.getSpendingByCategory(eq(currentUser), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(new Object[]{"Dining", 36000L}));
        when(openAiClient.generateBudgetRecommendations(anyString(), anyString()))
            .thenThrow(new RuntimeException("OpenAI unavailable"));

        GenerateBudgetRequest request = new GenerateBudgetRequest(
            "2025-09",
            List.of("Save more"),
            "balanced",
            null,
            null
        );

        aiBudgetService.generateBudget(request);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> aiBudgetService.generateBudget(request));
    }

    @Test
    void acceptBudget_persistsTargetsInCents() {
        AcceptBudgetRequest request = new AcceptBudgetRequest(
            "2025-09",
            List.of(new GenerateBudgetResponse.CategoryTarget("Dining", BigDecimal.valueOf(300), "Reason"))
        );

        AcceptBudgetResponse response = aiBudgetService.acceptBudget(request);

        assertEquals("ok", response.getStatus());
        verify(budgetTargetRepository).deleteByUserIdAndMonth(currentUser.getId(), "2025-09");
        verify(budgetTargetRepository).saveAll(argThat(list ->
            list.size() == 1 &&
            list.get(0).getCategory().equals("Dining") &&
            list.get(0).getTargetCents().equals(30000)
        ));
    }

    private OpenAiClient.OpenAiChoice createOpenAiChoice() {
        OpenAiClient.OpenAiChoice choice = new OpenAiClient.OpenAiChoice();
        choice.message = new OpenAiClient.OpenAiMessage(
            "assistant",
            "{\"targetsByCategory\":[{\"category\":\"Dining\",\"target\":250,\"reason\":\"Cap\"}],\"summary\":{\"savingsRate\":0.2,\"notes\":[\"test\"]}}"
        );
        return choice;
    }

    private OpenAiClient.OpenAiUsage createOpenAiUsage() {
        OpenAiClient.OpenAiUsage usage = new OpenAiClient.OpenAiUsage();
        usage.promptTokens = 100;
        usage.completionTokens = 50;
        usage.totalTokens = 150;
        return usage;
    }
}
