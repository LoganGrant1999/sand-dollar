package com.sanddollar.service.impl;

import com.sanddollar.dto.aibudget.*;
import com.sanddollar.entity.BudgetTarget;
import com.sanddollar.entity.User;
import com.sanddollar.repository.BudgetTargetRepository;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.repository.BalanceSnapshotRepository;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.OpenAiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiBudgetServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BalanceSnapshotRepository balanceSnapshotRepository;

    @Mock
    private BudgetTargetRepository budgetTargetRepository;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private AiBudgetServiceImpl aiBudgetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getUser()).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetFinancialSnapshot() {
        // Given
        List<Object[]> spendingData = Arrays.asList(
            new Object[]{"Food", 15000L},      // $150.00
            new Object[]{"Transport", 8000L},   // $80.00
            new Object[]{"Entertainment", 5000L} // $50.00
        );
        
        when(transactionRepository.getSpendingByCategory(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(spendingData);

        // When
        FinancialSnapshotResponse response = aiBudgetService.getFinancialSnapshot();

        // Then
        assertNotNull(response);
        assertEquals("2025-09", response.getMonth());
        assertEquals(new BigDecimal("6200.00"), response.getIncome()); // Default income estimate
        assertEquals(3, response.getActualsByCategory().size());
        
        // Verify category spending
        assertEquals("Food", response.getActualsByCategory().get(0).getCategory());
        assertEquals(new BigDecimal("150.00"), response.getActualsByCategory().get(0).getActual());
        
        // Verify totals
        assertEquals(new BigDecimal("280.00"), response.getTotals().getExpenses());
        assertEquals(new BigDecimal("5920.00"), response.getTotals().getSavings());
        
        verify(transactionRepository).getSpendingByCategory(eq(testUser), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testGenerateBudget_Success() {
        // Given
        GenerateBudgetRequest request = new GenerateBudgetRequest();
        request.setMonth("2025-10");
        request.setGoals(Arrays.asList("Build emergency fund", "Reduce expenses"));
        request.setStyle("balanced");
        request.setNotes("Looking to save more money");

        List<Object[]> historicalData = Arrays.asList(
            new Object[]{"Food", 18000L},
            new Object[]{"Transport", 10000L}
        );
        
        when(transactionRepository.getSpendingByCategory(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(historicalData);

        // Mock OpenAI response
        OpenAiClient.OpenAiResponse aiResponse = new OpenAiClient.OpenAiResponse();
        aiResponse.choices = Arrays.asList(createOpenAiChoice());
        aiResponse.usage = createOpenAiUsage();
        
        when(openAiClient.generateBudgetRecommendations(anyString(), anyString())).thenReturn(aiResponse);

        // When
        GenerateBudgetResponse response = aiBudgetService.generateBudget(request);

        // Then
        assertNotNull(response);
        assertEquals("2025-10", response.getMonth());
        assertNotNull(response.getTargetsByCategory());
        assertTrue(response.getTargetsByCategory().size() > 0);
        assertNotNull(response.getSummary());
        
        verify(openAiClient).generateBudgetRecommendations(anyString(), anyString());
        verify(transactionRepository).getSpendingByCategory(eq(testUser), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testGenerateBudget_AiFailure_UsesFallback() {
        // Given
        GenerateBudgetRequest request = new GenerateBudgetRequest();
        request.setMonth("2025-10");
        request.setGoals(Arrays.asList("Save money"));
        request.setStyle("aggressive");

        List<Object[]> historicalData = Arrays.asList(
            new Object[]{"Food", 20000L},
            new Object[]{"Entertainment", 5000L}
        );
        
        when(transactionRepository.getSpendingByCategory(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(historicalData);

        // Mock OpenAI failure
        when(openAiClient.generateBudgetRecommendations(anyString(), anyString()))
            .thenThrow(new RuntimeException("OpenAI API failure"));

        // When
        GenerateBudgetResponse response = aiBudgetService.generateBudget(request);

        // Then
        assertNotNull(response);
        assertEquals("2025-10", response.getMonth());
        assertNotNull(response.getTargetsByCategory());
        
        // Should have fallback targets based on historical data
        assertTrue(response.getTargetsByCategory().size() > 0);
        assertEquals(new BigDecimal("0.15"), response.getSummary().getSavingsRate());
        assertTrue(response.getSummary().getNotes().get(0).contains("Fallback budget"));
        
        verify(openAiClient).generateBudgetRecommendations(anyString(), anyString());
    }

    @Test
    void testAcceptBudget_Success() {
        // Given
        AcceptBudgetRequest request = new AcceptBudgetRequest();
        request.setMonth("2025-10");
        
        List<GenerateBudgetResponse.CategoryTarget> targets = Arrays.asList(
            new GenerateBudgetResponse.CategoryTarget("Food", new BigDecimal("400.00"), "Balanced food budget"),
            new GenerateBudgetResponse.CategoryTarget("Transport", new BigDecimal("200.00"), "Public transport focus")
        );
        request.setTargetsByCategory(targets);

        // When
        AcceptBudgetResponse response = aiBudgetService.acceptBudget(request);

        // Then
        assertNotNull(response);
        assertEquals("ok", response.getStatus());
        
        // Verify budget targets were saved
        verify(budgetTargetRepository).deleteByUserIdAndMonth(testUser.getId(), "2025-10");
        verify(budgetTargetRepository).saveAll(argThat((List<BudgetTarget> budgetTargets) -> {
            return budgetTargets.size() == 2 &&
                   budgetTargets.get(0).getCategory().equals("Food") &&
                   budgetTargets.get(0).getTargetCents().equals(40000) && // $400.00 in cents
                   budgetTargets.get(1).getCategory().equals("Transport") &&
                   budgetTargets.get(1).getTargetCents().equals(20000); // $200.00 in cents
        }));
    }

    @Test
    void testAcceptBudget_RepositoryFailure() {
        // Given
        AcceptBudgetRequest request = new AcceptBudgetRequest();
        request.setMonth("2025-10");
        request.setTargetsByCategory(Arrays.asList(
            new GenerateBudgetResponse.CategoryTarget("Food", new BigDecimal("400.00"), "Test")
        ));

        // Mock repository failure
        doThrow(new RuntimeException("Database error")).when(budgetTargetRepository).saveAll(any());

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiBudgetService.acceptBudget(request);
        });
        
        assertTrue(exception.getMessage().contains("Failed to save budget targets"));
        verify(budgetTargetRepository).deleteByUserIdAndMonth(testUser.getId(), "2025-10");
    }

    @Test
    void testFallbackBudget_AppliesStyleAdjustments() {
        // Given
        GenerateBudgetRequest aggressiveRequest = new GenerateBudgetRequest();
        aggressiveRequest.setMonth("2025-10");
        aggressiveRequest.setGoals(Arrays.asList("Save aggressively"));
        aggressiveRequest.setStyle("aggressive");

        List<Object[]> historicalData = Arrays.<Object[]>asList(
            new Object[]{"Food", 20000L} // $200.00
        );
        
        when(transactionRepository.getSpendingByCategory(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(historicalData);

        // Mock OpenAI failure to trigger fallback
        when(openAiClient.generateBudgetRecommendations(anyString(), anyString()))
            .thenThrow(new RuntimeException("AI failure"));

        // When
        GenerateBudgetResponse response = aiBudgetService.generateBudget(aggressiveRequest);

        // Then
        assertNotNull(response);
        
        // For aggressive style, should apply 15% reduction (0.85 factor)
        // $200.00 * 0.85 = $170.00
        GenerateBudgetResponse.CategoryTarget foodTarget = response.getTargetsByCategory().stream()
            .filter(t -> t.getCategory().equals("Food"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(foodTarget);
        assertEquals(new BigDecimal("170.00"), foodTarget.getTarget());
        assertTrue(foodTarget.getReason().contains("aggressive"));
    }

    private OpenAiClient.OpenAiChoice createOpenAiChoice() {
        OpenAiClient.OpenAiChoice choice = new OpenAiClient.OpenAiChoice();
        choice.message = new OpenAiClient.OpenAiMessage("assistant", 
            "{\"targetsByCategory\":[{\"category\":\"Food\",\"target\":350.00,\"reason\":\"Balanced food budget\"}," +
            "{\"category\":\"Transport\",\"target\":150.00,\"reason\":\"Public transport focus\"}]," +
            "\"summary\":{\"savingsRate\":0.20,\"notes\":[\"AI-generated balanced budget\"]}}");
        choice.finishReason = "stop";
        return choice;
    }

    private OpenAiClient.OpenAiUsage createOpenAiUsage() {
        OpenAiClient.OpenAiUsage usage = new OpenAiClient.OpenAiUsage();
        usage.promptTokens = 500;
        usage.completionTokens = 200;
        usage.totalTokens = 700;
        return usage;
    }
}