package com.sanddollar.service.impl;

import com.sanddollar.dto.aibudget.*;
import com.sanddollar.entity.BudgetTarget;
import com.sanddollar.entity.User;
import com.sanddollar.repository.BudgetTargetRepository;
import com.sanddollar.repository.TransactionRepository;
import com.sanddollar.repository.BalanceSnapshotRepository;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.AiBudgetRateLimiter;
import com.sanddollar.service.AiBudgetService;
import com.sanddollar.service.OpenAiClient;
import com.sanddollar.service.SpendingDataProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

    @Autowired
    private AiBudgetRateLimiter rateLimiter;

    @Autowired(required = false)
    @Qualifier("plaidSpendingDataProvider")
    private SpendingDataProvider plaidSpendingDataProvider;

    @Autowired
    @Qualifier("spendingDataProviderFallback")
    private SpendingDataProvider fallbackSpendingDataProvider;

    @Value("${feature.ai-budget-enabled:true}")
    private boolean aiBudgetEnabled;
    
    @Override
    public FinancialSnapshotResponse getFinancialSnapshot() {
        User user = getCurrentUser();
        SpendingDataProvider activeProvider = plaidSpendingDataProvider != null ? plaidSpendingDataProvider : fallbackSpendingDataProvider;
        SpendingDataProvider.SnapshotDto snapshot = activeProvider.getCurrentMonthSnapshot(
            user.getId(),
            java.time.ZoneId.of("America/Denver")
        );

        String currentMonth = snapshot.month() != null
            ? snapshot.month()
            : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal income = snapshot.income() != null ? snapshot.income() : BigDecimal.ZERO;
        List<FinancialSnapshotResponse.CategoryActual> actualsByCategory = snapshot.actualsByCategory() != null
            ? new ArrayList<>(snapshot.actualsByCategory())
            : new ArrayList<>();

        List<BudgetTarget> savedTargets = budgetTargetRepository.findByUserIdAndMonthOrderByCategory(user.getId(), currentMonth);
        Map<String, BudgetTarget> targetMap = savedTargets.stream()
            .collect(Collectors.toMap(BudgetTarget::getCategory, target -> target, (existing, replacement) -> replacement));

        for (FinancialSnapshotResponse.CategoryActual actual : actualsByCategory) {
            BudgetTarget target = targetMap.remove(actual.getCategory());
            if (target != null) {
                actual.setTarget(centsToDollars(target.getTargetCents()));
            }
        }

        for (BudgetTarget remaining : targetMap.values()) {
            actualsByCategory.add(new FinancialSnapshotResponse.CategoryActual(
                remaining.getCategory(),
                BigDecimal.ZERO,
                centsToDollars(remaining.getTargetCents())
            ));
        }

        actualsByCategory.sort(Comparator.comparing(FinancialSnapshotResponse.CategoryActual::getCategory));

        // Calculate totals
        BigDecimal totalExpenses = actualsByCategory.stream()
            .map(FinancialSnapshotResponse.CategoryActual::getActual)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal savings = income.subtract(totalExpenses);
        BigDecimal netCashFlow = savings;

        FinancialSnapshotResponse.FinancialTotals totals = new FinancialSnapshotResponse.FinancialTotals(
            totalExpenses, savings, netCashFlow
        );

        List<FinancialSnapshotResponse.CategoryTarget> targetDtos = savedTargets.stream()
            .map(target -> new FinancialSnapshotResponse.CategoryTarget(
                target.getCategory(),
                centsToDollars(target.getTargetCents()),
                Optional.ofNullable(target.getReason()).orElse("Saved AI target")
            ))
            .collect(Collectors.toList());

        Instant acceptedAt = savedTargets.stream()
            .map(BudgetTarget::getCreatedAt)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);

        return new FinancialSnapshotResponse(currentMonth, income, actualsByCategory, totals, targetDtos, acceptedAt);
    }
    
    @Override
    public GenerateBudgetResponse generateBudget(GenerateBudgetRequest request) {
        User user = getCurrentUser();

        if (!rateLimiter.tryConsume(user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                "You can request a new AI plan in about a minute."
            );
        }

        try {
            HistoricalData historicalData = getHistoricalData(user);

            if (!aiBudgetEnabled) {
                logger.info("ai_budget.generate.fallback user={} reason=disabled", user.getId());
                return generateFallbackBudget(request, user, historicalData);
            }

            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request, historicalData);

            long start = System.nanoTime();
            OpenAiClient.OpenAiResponse aiResponse = openAiClient.generateBudgetRecommendations(systemPrompt, userPrompt);
            long latencyMs = java.time.Duration.ofNanos(System.nanoTime() - start).toMillis();

            Optional<GenerateBudgetResponse> parsed = parseAiResponse(request.getMonth(), aiResponse);
            if (parsed.isPresent()) {
                int promptTokens = aiResponse.usage != null ? aiResponse.usage.promptTokens : 0;
                int completionTokens = aiResponse.usage != null ? aiResponse.usage.completionTokens : 0;
                logger.info("ai_budget.generate.success user={} latencyMs={} promptTokens={} completionTokens={}",
                    user.getId(), latencyMs, promptTokens, completionTokens);
                return parsed.get();
            }

            logger.warn("ai_budget.generate.invalid_json user={}", user.getId());
            return generateFallbackBudget(request, user, historicalData);

        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("ai_budget.generate.error user={} message={}", user.getId(), e.getMessage());
            HistoricalData historicalData = getHistoricalData(user);
            return generateFallbackBudget(request, user, historicalData);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authentication context available");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new IllegalStateException("Expected UserPrincipal but got: " +
                (principal != null ? principal.getClass().getName() : "null"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
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

        BigDecimal estimatedIncome = calculateMonthlyIncome(user, startDate, endDate);
        
        return new HistoricalData(avgSpending, estimatedIncome);
    }
    
    private String buildSystemPrompt() {
        return """
            You are Sand Dollar, an AI budgeting coach.
            \nSand Dollar philosophy:
            - Start from a 50/30/20 foundation (needs/wants/savings) but adapt per person.
            - Keep monthly spending targets as whole dollars (no decimals).
            - Balance the user's recent 90-day spending with their goals and chosen style.
            - Always respect must-keep categories and any category caps provided.
            - Encourage savings progress while keeping the plan realistic.
            - For each category include a single concise sentence explaining the recommendation.
            \nFormatting rules:
            - Output strictly valid JSON, no markdown, no extra commentary.
            - Match exactly this schema:
              {
                "targetsByCategory": [
                  {"category": "string", "target": 0, "reason": "string"}
                ],
                "summary": {
                  "savingsRate": 0.0,
                  "notes": ["string"]
                }
              }
            - "target" must be a non-negative integer (whole dollars).
            - "savingsRate" is a decimal between 0 and 1 inclusive.
            - Provide at least one note summarizing the overall approach.
            - If you must allocate funds to a category not seen in historical data, still keep the target as an integer.
            """;
    }

    private String buildUserPrompt(GenerateBudgetRequest request, HistoricalData historicalData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Month: ").append(request.getMonth()).append("\n");

        BigDecimal monthlyIncome = historicalData.estimatedIncome;
        prompt.append("Net monthly income: $").append(monthlyIncome).append("\n\n");

        prompt.append("Average spend by category over last 90 days (monthly):\n");
        historicalData.avgSpending.forEach((category, amount) ->
            prompt.append("- ").append(category).append(": $").append(amount).append("\n"));

        prompt.append("\nGoals:\n");
        for (String goal : request.getGoals()) {
            prompt.append("- ").append(goal).append("\n");
        }

        prompt.append("\nStyle: ").append(request.getStyle()).append("\n");

        if (request.getConstraints() != null) {
            if (request.getConstraints().getMustKeepCategories() != null && !request.getConstraints().getMustKeepCategories().isEmpty()) {
                prompt.append("Must keep categories (minimize changes): ")
                    .append(String.join(", ", request.getConstraints().getMustKeepCategories()))
                    .append("\n");
            }
            if (request.getConstraints().getCategoryCaps() != null && !request.getConstraints().getCategoryCaps().isEmpty()) {
                prompt.append("\nCategory caps (hard maximum):\n");
                request.getConstraints().getCategoryCaps().forEach((category, cap) ->
                    prompt.append("- ").append(category).append(": $").append(cap).append("\n"));
            }
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            prompt.append("\nAdditional context: ").append(request.getNotes().trim()).append("\n");
        }

        prompt.append("\nPlease respond with JSON only.");
        return prompt.toString();
    }

    private Optional<GenerateBudgetResponse> parseAiResponse(String month, OpenAiClient.OpenAiResponse aiResponse) {
        String content = aiResponse.getContent();
        if (content == null) {
            logger.warn("Empty AI response content");
            return Optional.empty();
        }

        String cleaned = content
            .replaceAll("```json", "")
            .replaceAll("```", "")
            .trim();

        Optional<ObjectNode> jsonNode = extractJson(cleaned);
        if (jsonNode.isEmpty()) {
            logger.warn("Unable to locate JSON object in AI response");
            return Optional.empty();
        }

        ObjectNode root = jsonNode.get();
        ValidationResult validation = validateResponseSchema(root);
        if (!validation.ok()) {
            logger.warn("AI response failed schema validation: {}", validation.message());
            return Optional.empty();
        }

        ArrayNode targetsNode = (ArrayNode) root.get("targetsByCategory");
        List<GenerateBudgetResponse.CategoryTarget> targets = new ArrayList<>();
        for (JsonNode node : targetsNode) {
            String category = node.get("category").asText();
            int targetWhole = node.get("target").asInt();
            String reason = node.get("reason").asText();
            targets.add(new GenerateBudgetResponse.CategoryTarget(
                category,
                new BigDecimal(targetWhole),
                reason
            ));
        }

        ObjectNode summaryNode = (ObjectNode) root.get("summary");
        BigDecimal savingsRate = summaryNode.get("savingsRate").decimalValue().setScale(3, RoundingMode.HALF_UP);
        List<String> notes = new ArrayList<>();
        summaryNode.withArray("notes").forEach(note -> notes.add(note.asText()));

        GenerateBudgetResponse.BudgetSummary summary = new GenerateBudgetResponse.BudgetSummary(savingsRate, notes);

        return Optional.of(new GenerateBudgetResponse(
            month,
            targets,
            summary,
            aiResponse.usage != null ? aiResponse.usage.promptTokens : 0,
            aiResponse.usage != null ? aiResponse.usage.completionTokens : 0
        ));
    }

    private Optional<ObjectNode> extractJson(String content) {
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace == -1 || lastBrace == -1 || firstBrace > lastBrace) {
            return Optional.empty();
        }
        String jsonSegment = content.substring(firstBrace, lastBrace + 1);
        try {
            JsonNode node = objectMapper.readTree(jsonSegment);
            if (node instanceof ObjectNode objectNode) {
                return Optional.of(objectNode);
            }
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse JSON segment", e);
        }
        return Optional.empty();
    }

    private ValidationResult validateResponseSchema(ObjectNode root) {
        if (!root.has("targetsByCategory") || !root.get("targetsByCategory").isArray()) {
            return ValidationResult.failure("Missing targetsByCategory array");
        }
        ArrayNode targetsNode = (ArrayNode) root.get("targetsByCategory");
        if (targetsNode.isEmpty()) {
            return ValidationResult.failure("targetsByCategory must contain at least one entry");
        }
        for (JsonNode node : targetsNode) {
            if (!node.hasNonNull("category") || !node.get("category").isTextual()) {
                return ValidationResult.failure("Each target requires a category string");
            }
            if (!node.has("target") || !node.get("target").canConvertToInt()) {
                return ValidationResult.failure("Each target requires an integer 'target' amount");
            }
            if (node.get("target").asInt() < 0) {
                return ValidationResult.failure("Target amounts must be non-negative integers");
            }
            if (!node.hasNonNull("reason") || !node.get("reason").isTextual()) {
                return ValidationResult.failure("Each target requires a reason");
            }
        }

        if (!root.has("summary") || !root.get("summary").isObject()) {
            return ValidationResult.failure("Missing summary object");
        }

        JsonNode summary = root.get("summary");
        if (!summary.has("savingsRate") || !summary.get("savingsRate").isNumber()) {
            return ValidationResult.failure("Summary must include numeric savingsRate");
        }
        double rate = summary.get("savingsRate").asDouble();
        if (rate < 0 || rate > 1) {
            return ValidationResult.failure("savingsRate must be between 0 and 1");
        }
        if (!summary.has("notes") || !summary.get("notes").isArray()) {
            return ValidationResult.failure("Summary must include notes array");
        }
        return ValidationResult.success();
    }

    private GenerateBudgetResponse generateFallbackBudget(GenerateBudgetRequest request, User user, HistoricalData historicalData) {
        logger.info("ai_budget.generate.fallback user={} reason=heuristic", user.getId());

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
        
        BigDecimal income = historicalData.estimatedIncome;
        BigDecimal totalTargets = targets.stream()
            .map(GenerateBudgetResponse.CategoryTarget::getTarget)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal savingsRate = income.compareTo(BigDecimal.ZERO) > 0
            ? income.subtract(totalTargets).max(BigDecimal.ZERO).divide(income, 3, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        GenerateBudgetResponse.BudgetSummary summary = new GenerateBudgetResponse.BudgetSummary(
            savingsRate,
            List.of("Heuristic budget based on historical spending patterns")
        );
        
        return new GenerateBudgetResponse(request.getMonth(), targets, summary, 0, 0);
    }

    private BigDecimal centsToDollars(Integer cents) {
        return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private static class HistoricalData {
        final Map<String, BigDecimal> avgSpending;
        final BigDecimal estimatedIncome;

        HistoricalData(Map<String, BigDecimal> avgSpending, BigDecimal estimatedIncome) {
            this.avgSpending = avgSpending;
            this.estimatedIncome = estimatedIncome;
        }
    }

    private record ValidationResult(boolean ok, String message) {
        static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }
}
