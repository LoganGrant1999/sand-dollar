package com.sanddollar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanddollar.dto.aibudget.AcceptBudgetRequest;
import com.sanddollar.dto.aibudget.AcceptBudgetResponse;
import com.sanddollar.dto.aibudget.GenerateBudgetRequest;
import com.sanddollar.dto.aibudget.GenerateBudgetResponse;
import com.sanddollar.dto.aibudget.FinancialSnapshotResponse;
import com.sanddollar.service.AiBudgetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiBudgetController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiBudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiBudgetService aiBudgetService;

    @Test
    void generateBudget_invalidRequest_returnsBadRequest() throws Exception {
        GenerateBudgetRequest request = new GenerateBudgetRequest(
            "", // invalid month
            List.of(),
            "balanced",
            null,
            null
        );

        mockMvc.perform(post("/api/ai/budget/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void generateBudget_returnsResponse() throws Exception {
        GenerateBudgetResponse response = new GenerateBudgetResponse(
            "2025-01",
            List.of(new GenerateBudgetResponse.CategoryTarget("Dining", BigDecimal.valueOf(300), "Cap dining")),
            new GenerateBudgetResponse.BudgetSummary(BigDecimal.valueOf(0.2), List.of("Test")),
            10,
            5
        );

        when(aiBudgetService.generateBudget(any(GenerateBudgetRequest.class))).thenReturn(response);

        GenerateBudgetRequest request = new GenerateBudgetRequest(
            "2025-01",
            List.of("Save money"),
            "balanced",
            null,
            null
        );

        mockMvc.perform(post("/api/ai/budget/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.month").value("2025-01"))
            .andExpect(jsonPath("$.targetsByCategory[0].category").value("Dining"));
    }

    @Test
    void snapshot_returnsFinancialSnapshot() throws Exception {
        FinancialSnapshotResponse snapshot = new FinancialSnapshotResponse(
            "2025-01",
            BigDecimal.valueOf(6200),
            List.of(new FinancialSnapshotResponse.CategoryActual("Dining", BigDecimal.valueOf(280))),
            new FinancialSnapshotResponse.FinancialTotals(
                BigDecimal.valueOf(1500),
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(200)
            ),
            List.of(new FinancialSnapshotResponse.CategoryTarget("Dining", BigDecimal.valueOf(300), "Cap dining")),
            null
        );

        when(aiBudgetService.getFinancialSnapshot()).thenReturn(snapshot);

        mockMvc.perform(post("/api/ai/budget/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.targetsByCategory[0].category").value("Dining"));
    }

    @Test
    void acceptBudget_returnsOk() throws Exception {
        when(aiBudgetService.acceptBudget(any(AcceptBudgetRequest.class)))
            .thenReturn(new AcceptBudgetResponse("ok"));

        AcceptBudgetRequest request = new AcceptBudgetRequest(
            "2025-01",
            List.of(new GenerateBudgetResponse.CategoryTarget("Dining", BigDecimal.valueOf(300), "Cap dining"))
        );

        mockMvc.perform(post("/api/ai/budget/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }
}
