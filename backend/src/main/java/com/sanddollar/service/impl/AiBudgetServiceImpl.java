package com.sanddollar.service.impl;

import com.sanddollar.dto.aibudget.*;
import com.sanddollar.entity.BudgetTarget;
import com.sanddollar.entity.User;
import com.sanddollar.repository.BudgetTargetRepository;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.repository.BalanceSnapshotRepository;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.AiBudgetService;
import com.sanddollar.service.OpenAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
public class AiBudgetServiceImpl implements AiBudgetService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiBudgetServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BalanceSnapshotRepository balanceSnapshotRepository;
    
    @Autowired
    private BudgetTargetRepository budgetTargetRepository;
    
    @Autowired
    private OpenAiClient openAiClient;
    
    @Override
    public FinancialSnapshotResponse getFinancialSnapshot() {
        User user = getCurrentUser();
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // Get current month date range
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now();
        
        // Get income estimate (positive transactions this month)
        BigDecimal income = calculateMonthlyIncome(user, startOfMonth, endOfMonth);
        
        // Get spending by category
        List<FinancialSnapshotResponse.CategoryActual> actualsByCategory = getSpendingByCategory(user, startOfMonth, endOfMonth);
        
        // Calculate totals
        BigDecimal totalExpenses = actualsByCategory.stream()
            .map(FinancialSnapshotResponse.CategoryActual::getActual)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal savings = income.subtract(totalExpenses);
        BigDecimal netCashFlow = savings; // Simplified for now
        
        FinancialSnapshotResponse.FinancialTotals totals = new FinancialSnapshotResponse.FinancialTotals(
            totalExpenses, savings, netCashFlow
        );
        
        return new FinancialSnapshotResponse(currentMonth, income, actualsByCategory, totals);
    }
    
    @Override
    public GenerateBudgetResponse generateBudget(GenerateBudgetRequest request) {
        User user = getCurrentUser();
        
        try {
            // Get historical spending data for context
            HistoricalData historicalData = getHistoricalData(user);
            
            // Build AI prompt
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request, historicalData);
            
            // Call OpenAI API
            OpenAiClient.OpenAiResponse aiResponse = openAiClient.generateBudgetRecommendations(systemPrompt, userPrompt);
            
            // Parse AI response
            return parseAiResponse(request.getMonth(), aiResponse);
            
        } catch (Exception e) {
            logger.warn("AI budget generation failed, using fallback", e);
            return generateFallbackBudget(request, user);
        }
    }
    
    @Override
    @Transactional
    public AcceptBudgetResponse acceptBudget(AcceptBudgetRequest request) {
        User user = getCurrentUser();
        
        try {
            // Delete existing targets for this month
            budgetTargetRepository.deleteByUserIdAndMonth(user.getId(), request.getMonth());
            
            // Save new targets
            List<BudgetTarget> targets = request.getTargetsByCategory().stream()
                .map(target -> new BudgetTarget(
                    user.getId(),
                    request.getMonth(),
                    target.getCategory(),
                    target.getTarget().multiply(new BigDecimal("100")).intValue(), // Convert to cents
                    target.getReason()
                ))
                .collect(Collectors.toList());
            
            budgetTargetRepository.saveAll(targets);
            
            logger.info("Saved {} budget targets for user {} month {}", 
                targets.size(), user.getId(), request.getMonth());
            
            return new AcceptBudgetResponse("ok");
            
        } catch (Exception e) {
            logger.error("Failed to accept budget", e);
            throw new RuntimeException("Failed to save budget targets: " + e.getMessage());
        }
    }
    
    private User getCurrentUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        return userPrincipal.getUser();
    }
    
    private BigDecimal calculateMonthlyIncome(User user, LocalDate startDate, LocalDate endDate) {
        // Get positive transactions (income) - simplified estimation
        List<Object[]> incomeData = transactionRepository.getSpendingByCategory(user, startDate, endDate);
        
        // For now, estimate based on typical income patterns or use a default
        // In production, you'd want a more sophisticated income detection
        return new BigDecimal("6200.00"); // Default estimate
    }
    
    private List<FinancialSnapshotResponse.CategoryActual> getSpendingByCategory(User user, LocalDate startDate, LocalDate endDate) {
        List<Object[]> spendingData = transactionRepository.getSpendingByCategory(user, startDate, endDate);
        
        return spendingData.stream()
            .map(row -> {
                String category = (String) row[0];
                Long amountCents = (Long) row[1];
                BigDecimal amount = new BigDecimal(amountCents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                return new FinancialSnapshotResponse.CategoryActual(category, amount);
            })
            .collect(Collectors.toList());
    }
    
    private HistoricalData getHistoricalData(User user) {
        // Get last 90 days of spending
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);
        
        List<Object[]> spendingData = transactionRepository.getSpendingByCategory(user, startDate, endDate);
        
        Map<String, BigDecimal> avgSpending = spendingData.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> new BigDecimal((Long) row[1]).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP) // 3 months avg
            ));
        
        return new HistoricalData(avgSpending);
    }
    
    private String buildSystemPrompt() {
        return """
            You are a financial advisor AI helping users create personalized budgets. 
            
            Your task is to analyze the user's financial situation, goals, and preferences to recommend budget targets by category.
            
            IMPORTANT: Respond ONLY with valid JSON matching this exact schema:
            {
              "targetsByCategory": [
                {"category": "string", "target": number, "reason": "string"}
              ],
              "summary": {
                "savingsRate": number_between_0_and_1,
                "notes": ["string"]
              }
            }
            
            Guidelines:
            - Base recommendations on historical spending patterns
            - Adjust for user goals (emergency fund, debt payment, etc.)
            - Consider user style preference (aggressive/balanced/flexible)
            - Respect any category constraints (must keep, caps)
            - Provide clear, actionable reasons for each target
            - Aim for realistic targets that user can achieve
            - Include both expense categories and savings/goal categories
            """;
    }
    
    private String buildUserPrompt(GenerateBudgetRequest request, HistoricalData historicalData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please create a budget for ").append(request.getMonth()).append("\\n\\n");
        
        prompt.append("USER GOALS:\\n");
        for (String goal : request.getGoals()) {
            prompt.append("- ").append(goal).append("\\n");
        }
        
        prompt.append("\\nBUDGET STYLE: ").append(request.getStyle()).append("\\n");
        
        if (request.getConstraints() != null) {
            if (request.getConstraints().getMustKeepCategories() != null) {
                prompt.append("\\nMUST KEEP CATEGORIES: ")
                    .append(String.join(", ", request.getConstraints().getMustKeepCategories()))
                    .append("\\n");
            }
            
            if (request.getConstraints().getCategoryCaps() != null) {
                prompt.append("\\nCATEGORY CAPS:\\n");
                request.getConstraints().getCategoryCaps().forEach((category, cap) ->
                    prompt.append("- ").append(category).append(": $").append(cap).append("\\n"));
            }
        }
        
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            prompt.append("\\nADDITIONAL NOTES: ").append(request.getNotes()).append("\\n");
        }
        
        prompt.append("\\nHISTORICAL SPENDING (last 90 days average):\\n");
        historicalData.avgSpending.forEach((category, amount) ->
            prompt.append("- ").append(category).append(": $").append(amount).append("\\n"));
        
        return prompt.toString();
    }
    
    private GenerateBudgetResponse parseAiResponse(String month, OpenAiClient.OpenAiResponse aiResponse) {
        try {
            String content = aiResponse.getContent();
            if (content == null) {
                throw new RuntimeException("Empty response from AI");
            }
            
            // Clean the response (remove markdown formatting if present)
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            
            JsonNode responseJson = objectMapper.readTree(content);
            
            List<GenerateBudgetResponse.CategoryTarget> targets = new ArrayList<>();
            JsonNode targetsNode = responseJson.get("targetsByCategory");
            
            if (targetsNode != null && targetsNode.isArray()) {
                for (JsonNode targetNode : targetsNode) {
                    targets.add(new GenerateBudgetResponse.CategoryTarget(
                        targetNode.get("category").asText(),
                        new BigDecimal(targetNode.get("target").asText()),
                        targetNode.get("reason").asText()
                    ));
                }
            }
            
            GenerateBudgetResponse.BudgetSummary summary = null;
            JsonNode summaryNode = responseJson.get("summary");
            if (summaryNode != null) {
                BigDecimal savingsRate = new BigDecimal(summaryNode.get("savingsRate").asText());
                List<String> notes = new ArrayList<>();
                JsonNode notesNode = summaryNode.get("notes");
                if (notesNode != null && notesNode.isArray()) {
                    for (JsonNode noteNode : notesNode) {
                        notes.add(noteNode.asText());
                    }
                }
                summary = new GenerateBudgetResponse.BudgetSummary(savingsRate, notes);
            }
            
            return new GenerateBudgetResponse(
                month,
                targets,
                summary != null ? summary : new GenerateBudgetResponse.BudgetSummary(new BigDecimal("0.15"), List.of("AI-generated budget")),
                aiResponse.usage != null ? aiResponse.usage.promptTokens : 0,
                aiResponse.usage != null ? aiResponse.usage.completionTokens : 0
            );
            
        } catch (Exception e) {
            logger.error("Failed to parse AI response", e);
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage());
        }
    }
    
    private GenerateBudgetResponse generateFallbackBudget(GenerateBudgetRequest request, User user) {
        logger.info("Generating fallback budget based on historical data");
        
        HistoricalData historicalData = getHistoricalData(user);
        
        List<GenerateBudgetResponse.CategoryTarget> targets = historicalData.avgSpending.entrySet().stream()
            .map(entry -> {
                String category = entry.getKey();
                BigDecimal historicalAmount = entry.getValue();
                
                // Apply style-based adjustments
                BigDecimal adjustmentFactor = switch (request.getStyle()) {
                    case "aggressive" -> new BigDecimal("0.85"); // 15% reduction
                    case "balanced" -> new BigDecimal("0.95");   // 5% reduction
                    case "flexible" -> new BigDecimal("1.05");   // 5% increase
                    default -> new BigDecimal("1.0");
                };
                
                BigDecimal target = historicalAmount.multiply(adjustmentFactor);
                
                // Apply constraints
                if (request.getConstraints() != null && request.getConstraints().getCategoryCaps() != null) {
                    BigDecimal cap = request.getConstraints().getCategoryCaps().get(category);
                    if (cap != null && target.compareTo(cap) > 0) {
                        target = cap;
                    }
                }
                
                String reason = "Based on 90-day average with " + request.getStyle() + " adjustment";
                return new GenerateBudgetResponse.CategoryTarget(category, target, reason);
            })
            .collect(Collectors.toList());
        
        // Add savings targets based on goals
        for (String goal : request.getGoals()) {
            if (goal.toLowerCase().contains("emergency") && goal.toLowerCase().contains("fund")) {
                targets.add(new GenerateBudgetResponse.CategoryTarget(
                    "Emergency Fund", 
                    new BigDecimal("500.00"), 
                    "Emergency fund building based on goal"
                ));
            }
            if (goal.toLowerCase().contains("debt")) {
                targets.add(new GenerateBudgetResponse.CategoryTarget(
                    "Debt Payment", 
                    new BigDecimal("200.00"), 
                    "Debt reduction based on goal"
                ));
            }
        }
        
        GenerateBudgetResponse.BudgetSummary summary = new GenerateBudgetResponse.BudgetSummary(
            new BigDecimal("0.15"),
            List.of("Fallback budget based on historical spending patterns")
        );
        
        return new GenerateBudgetResponse(request.getMonth(), targets, summary, 0, 0);
    }
    
    private static class HistoricalData {
        final Map<String, BigDecimal> avgSpending;
        
        HistoricalData(Map<String, BigDecimal> avgSpending) {
            this.avgSpending = avgSpending;
        }
    }
}