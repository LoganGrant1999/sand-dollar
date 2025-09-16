package com.sanddollar.controller;

import com.sanddollar.dto.aibudget.*;
import com.sanddollar.service.AiBudgetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiBudgetController.class)
@WithMockUser(username = "test@example.com", roles = "USER")
class AiBudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiBudgetService aiBudgetService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetFinancialSnapshot_Success() throws Exception {
        // Given
        FinancialSnapshotResponse mockResponse = new FinancialSnapshotResponse(
            "2025-09",
            new BigDecimal("6000.00"),
            Arrays.asList(
                new FinancialSnapshotResponse.CategoryActual("Food", new BigDecimal("400.00")),
                new FinancialSnapshotResponse.CategoryActual("Transport", new BigDecimal("200.00"))
            ),
            new FinancialSnapshotResponse.FinancialTotals(
                new BigDecimal("600.00"),
                new BigDecimal("5400.00"),
                new BigDecimal("5400.00")
            )
        );

        when(aiBudgetService.getFinancialSnapshot()).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/ai/budget/snapshot")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.month").value("2025-09"))
                .andExpect(jsonPath("$.income").value(6000.00))
                .andExpect(jsonPath("$.actuals_by_category").isArray())
                .andExpect(jsonPath("$.actuals_by_category.length()").value(2))
                .andExpect(jsonPath("$.actuals_by_category[0].category").value("Food"))
                .andExpect(jsonPath("$.actuals_by_category[0].actual").value(400.00))
                .andExpect(jsonPath("$.totals.expenses").value(600.00))
                .andExpect(jsonPath("$.totals.savings").value(5400.00));
    }

    @Test
    void testGenerateBudget_Success() throws Exception {
        // Given
        GenerateBudgetRequest request = new GenerateBudgetRequest();
        request.setMonth("2025-10");
        request.setGoals(Arrays.asList("Save money", "Reduce expenses"));
        request.setStyle("balanced");
        request.setNotes("Looking to optimize budget");

        GenerateBudgetResponse mockResponse = new GenerateBudgetResponse(
            "2025-10",
            Arrays.asList(
                new GenerateBudgetResponse.CategoryTarget("Food", new BigDecimal("350.00"), "Optimized food budget"),
                new GenerateBudgetResponse.CategoryTarget("Transport", new BigDecimal("150.00"), "Public transport focus")
            ),
            new GenerateBudgetResponse.BudgetSummary(
                new BigDecimal("0.20"),
                Arrays.asList("AI-generated balanced budget", "Focus on savings goals")
            ),
            450, // prompt tokens
            200  // completion tokens
        );

        when(aiBudgetService.generateBudget(any(GenerateBudgetRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/ai/budget/generate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.month").value("2025-10"))
                .andExpect(jsonPath("$.targets_by_category").isArray())
                .andExpect(jsonPath("$.targets_by_category.length()").value(2))
                .andExpect(jsonPath("$.targets_by_category[0].category").value("Food"))
                .andExpect(jsonPath("$.targets_by_category[0].target").value(350.00))
                .andExpect(jsonPath("$.targets_by_category[0].reason").value("Optimized food budget"))
                .andExpect(jsonPath("$.summary.savings_rate").value(0.20))
                .andExpect(jsonPath("$.summary.notes").isArray())
                .andExpect(jsonPath("$.prompt_tokens").value(450))
                .andExpect(jsonPath("$.completion_tokens").value(200));
    }

    @Test
    void testGenerateBudget_ValidationError() throws Exception {
        // Given - invalid request with missing required fields
        GenerateBudgetRequest invalidRequest = new GenerateBudgetRequest();
        // Missing month, goals, and style

        // When & Then
        mockMvc.perform(post("/api/ai/budget/generate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAcceptBudget_Success() throws Exception {
        // Given
        AcceptBudgetRequest request = new AcceptBudgetRequest();
        request.setMonth("2025-10");
        request.setTargetsByCategory(Arrays.asList(
            new GenerateBudgetResponse.CategoryTarget("Food", new BigDecimal("350.00"), "Balanced food budget"),
            new GenerateBudgetResponse.CategoryTarget("Transport", new BigDecimal("150.00"), "Transport optimization")
        ));

        AcceptBudgetResponse mockResponse = new AcceptBudgetResponse("ok");

        when(aiBudgetService.acceptBudget(any(AcceptBudgetRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/ai/budget/accept")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void testAcceptBudget_ValidationError() throws Exception {
        // Given - invalid request with missing month
        AcceptBudgetRequest invalidRequest = new AcceptBudgetRequest();
        invalidRequest.setTargetsByCategory(Arrays.asList(
            new GenerateBudgetResponse.CategoryTarget("Food", new BigDecimal("350.00"), "Test")
        ));
        // Missing month

        // When & Then
        mockMvc.perform(post("/api/ai/budget/accept")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // Test without authentication
        mockMvc.perform(post("/api/ai/budget/snapshot")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testServiceException() throws Exception {
        // Given
        when(aiBudgetService.getFinancialSnapshot()).thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/ai/budget/snapshot")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}