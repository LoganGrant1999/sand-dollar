package com.sanddollar.service;

import com.sanddollar.budgeting.BudgetBaselineService;
import com.sanddollar.dto.budget.BudgetOverviewDTO;
import com.sanddollar.dto.budget.CategoryRow;
import com.sanddollar.dto.aibudget.FinancialSnapshotResponse;
import com.sanddollar.entity.User;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.impl.SpendingDataProviderFallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetOverviewServiceTest {

    @Mock
    private SpendingDataProvider plaidSpendingDataProvider;

    @Mock
    private SpendingDataProviderFallback fallbackSpendingDataProvider;

    @Mock
    private BudgetBaselineService budgetBaselineService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private BudgetOverviewService budgetOverviewService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    void getBudgetOverview_ReturnsValidData() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userPrincipal);
            when(userPrincipal.getUser()).thenReturn(testUser);

            // Mock snapshot data
            List<FinancialSnapshotResponse.CategoryActual> categoryActuals = List.of(
                new FinancialSnapshotResponse.CategoryActual("Groceries", new BigDecimal("500.00")),
                new FinancialSnapshotResponse.CategoryActual("Transportation", new BigDecimal("200.00"))
            );

            SpendingDataProvider.SnapshotDto snapshotDto = mock(SpendingDataProvider.SnapshotDto.class);
            when(snapshotDto.month()).thenReturn("2025-09");
            when(snapshotDto.income()).thenReturn(new BigDecimal("5000.00"));
            when(snapshotDto.actualsByCategory()).thenReturn(categoryActuals);

            when(plaidSpendingDataProvider.getCurrentMonthSnapshot(eq(1L), any(ZoneId.class)))
                .thenReturn(snapshotDto);

            // Mock baseline data
            Map<String, Long> monthlyExpenses = new HashMap<>();
            monthlyExpenses.put("Groceries", 60000L); // $600.00 in cents
            monthlyExpenses.put("Transportation", 25000L); // $250.00 in cents

            Map<String, String> confidenceScores = new HashMap<>();
            confidenceScores.put("Groceries", "High");
            confidenceScores.put("Transportation", "Medium");

            BudgetBaselineService.BudgetBaseline baseline = new BudgetBaselineService.BudgetBaseline(
                480000L, // $4800.00 income in cents
                monthlyExpenses,
                85000L, // $850.00 total expenses in cents
                BudgetBaselineService.PaycheckCadence.BIWEEKLY,
                confidenceScores
            );

            when(budgetBaselineService.calculateBaseline(testUser)).thenReturn(baseline);

            // Act
            BudgetOverviewDTO result = budgetOverviewService.getBudgetOverview();

            // Assert
            assertNotNull(result);
            assertEquals("2025-09", result.monthIso());
            assertEquals(new BigDecimal("5000.00"), result.incomeMTD());
            assertEquals(new BigDecimal("700.00"), result.expensesMTD()); // 500 + 200
            assertEquals(new BigDecimal("4300.00"), result.netMTD()); // 5000 - 700
            assertEquals(0.86, result.savingsRateMTD(), 0.001); // 4300/5000

            assertEquals(new BigDecimal("4800.00"), result.incomeTypical());
            assertEquals(new BigDecimal("850.00"), result.expensesTypical());
            assertEquals(new BigDecimal("3950.00"), result.netTypical()); // 4800 - 850
            assertEquals(0.8229, result.savingsRateTypical(), 0.001); // 3950/4800

            assertNotNull(result.categoriesMTD());
            assertEquals(2, result.categoriesMTD().size());

            // Check first category (should be sorted by MTD amount desc)
            CategoryRow firstCategory = result.categoriesMTD().get(0);
            assertEquals("Groceries", firstCategory.key());
            assertEquals(new BigDecimal("500.00"), firstCategory.amountMTD());
            assertEquals(new BigDecimal("600.00"), firstCategory.amountTypical());
            assertEquals("High", firstCategory.confidence());

            // Verify mocks
            verify(plaidSpendingDataProvider).getCurrentMonthSnapshot(eq(1L), any(ZoneId.class));
            verify(budgetBaselineService).calculateBaseline(testUser);
        }
    }

    @Test
    void getBudgetOverview_HandlesMissingSnapshotData() {
        // Arrange
        try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userPrincipal);
            when(userPrincipal.getUser()).thenReturn(testUser);

            // Mock snapshot with null data
            SpendingDataProvider.SnapshotDto snapshotDto = mock(SpendingDataProvider.SnapshotDto.class);
            when(snapshotDto.month()).thenReturn(null);
            when(snapshotDto.income()).thenReturn(null);
            when(snapshotDto.actualsByCategory()).thenReturn(null);

            when(plaidSpendingDataProvider.getCurrentMonthSnapshot(eq(1L), any(ZoneId.class)))
                .thenReturn(snapshotDto);

            // Mock baseline data
            BudgetBaselineService.BudgetBaseline baseline = new BudgetBaselineService.BudgetBaseline(
                480000L,
                new HashMap<>(),
                0L,
                BudgetBaselineService.PaycheckCadence.MONTHLY,
                new HashMap<>()
            );

            when(budgetBaselineService.calculateBaseline(testUser)).thenReturn(baseline);

            // Act
            BudgetOverviewDTO result = budgetOverviewService.getBudgetOverview();

            // Assert
            assertNotNull(result);
            assertNotNull(result.monthIso()); // Should use current month
            assertEquals(BigDecimal.ZERO, result.incomeMTD());
            assertEquals(BigDecimal.ZERO, result.expensesMTD());
            assertEquals(BigDecimal.ZERO, result.netMTD());
            assertEquals(0.0, result.savingsRateMTD());
        }
    }
}