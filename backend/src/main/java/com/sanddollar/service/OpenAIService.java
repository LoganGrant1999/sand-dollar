package com.sanddollar.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.service.OpenAiService;
import com.sanddollar.dto.BudgetRequest;
import com.sanddollar.dto.BudgetPlanResponse;
import com.sanddollar.dto.Allocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    
    private final OpenAiService openAiService;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean mockMode;
    
    public OpenAIService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model}") String model) {
        logger.debug("OpenAI API Key received: '{}'", apiKey);
        this.mockMode = apiKey == null || apiKey.trim().isEmpty() || "mock-key".equals(apiKey);
        logger.debug("Mock mode determined: {}", mockMode);
        
        if (mockMode) {
            this.openAiService = null;
            logger.info("OpenAI service initialized in MOCK MODE (no API key provided)");
        } else {
            this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(12));
            logger.info("OpenAI service initialized with model: {} and 12s timeout", model);
        }
        this.model = model;
    }
    
    public String chatOnce(String prompt) {
        return chatOnce(List.of(
            new ChatMessage("system", "You are SandDollar's budgeting assistant. Be concise, practical, numerate. " +
                "When asked to change budgets, call /api/budgets/adjust (see below) and then confirm deltas. " +
                "Focus on actionable financial advice and budget optimization."),
            new ChatMessage("user", prompt)
        ), 0.7);
    }
    
    public String chatOnce(List<ChatMessage> messages, Double temperature) {
        if (mockMode) {
            logger.debug("Returning mock response for chat request with {} messages", messages.size());
            return "I'm your AI financial assistant! I'm currently running in demo mode. " +
                   "I can help you understand your spending patterns, create budgets, and provide financial insights. " +
                   "What would you like to know about your finances?";
        }
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Making OpenAI chat request with {} messages", messages.size());
                
                ChatCompletionRequest.ChatCompletionRequestBuilder builder = ChatCompletionRequest.builder()
                        .model(model)
                        .messages(messages);
                
                // Only set temperature if it's not the default value or if model supports it
                // Many newer models only support default temperature (1.0)
                if (temperature != null && !temperature.equals(1.0)) {
                    logger.debug("Skipping temperature parameter {} for model {} (may not be supported)", temperature, model);
                    // Don't set temperature to avoid API errors
                }
                
                ChatCompletionRequest completionRequest = builder.build();
                
                var response = openAiService.createChatCompletion(completionRequest);
                
                if (response.getChoices().isEmpty()) {
                    logger.error("No choices returned from OpenAI API");
                    throw new RuntimeException("No choices returned");
                }
                
                String result = response.getChoices().get(0).getMessage().getContent();
                logger.debug("OpenAI chat response: {}", result);
                return result;
                
            } catch (Exception e) {
                logger.error("Error calling OpenAI chat API", e);
                throw new RuntimeException(e);
            }
        });
        
        try {
            return future.get(12, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("OpenAI chat request timed out after 12 seconds");
            future.cancel(true);
            return "I'm taking longer than usual to respond. Let me give you a quick answer: I'm here to help with your budgeting questions. Could you try rephrasing your question or ask something more specific?";
        } catch (Exception e) {
            logger.error("Error in OpenAI chat request", e);
            return "Sorry, I encountered an issue processing your request. I'm still here to help - try asking about your budget, spending, or savings goals.";
        }
    }
    
    public void chatStream(String prompt, Consumer<String> onDelta, Runnable onComplete) {
        chatStream(List.of(
            new ChatMessage("system", "You are SandDollar's budgeting assistant. Be concise, practical, numerate. " +
                "When asked to change budgets, call /api/budgets/adjust (see below) and then confirm deltas. " +
                "Focus on actionable financial advice and budget optimization."),
            new ChatMessage("user", prompt)
        ), 0.7, onDelta, onComplete);
    }
    
    public void chatStream(List<ChatMessage> messages, Double temperature, Consumer<String> onDelta, Runnable onComplete) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Starting OpenAI streaming with {} messages", messages.size());
                
                // For now, we'll provide a better simulation that demonstrates real streaming capability
                // In production, this would use the actual OpenAI streaming API with proper API key
                String response = chatOnce(messages, temperature);
                
                // Stream the response word by word to simulate real streaming
                String[] words = response.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    if (i > 0) {
                        word = " " + word;
                    }
                    
                    try {
                        onDelta.accept(word);
                        Thread.sleep(50 + (int)(Math.random() * 100)); // Variable delay to simulate real typing
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                if (onComplete != null) {
                    onComplete.run();
                }
                
            } catch (Exception e) {
                logger.error("Error in streaming chat", e);
                onDelta.accept("Error: " + e.getMessage());
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
    
    public BudgetPlanResponse generateBudget(BudgetRequest request) {
        CompletableFuture<BudgetPlanResponse> future = CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Making OpenAI budget request: {}", request);
                
                String prompt = buildBudgetPrompt(request);
                
                ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                        .model(model)
                        .messages(List.of(
                            new ChatMessage("system", "You are a financial budgeting assistant. Return ONLY valid JSON matching the provided schema. Be concise. No explanatory text outside the JSON."),
                            new ChatMessage("user", prompt)
                        ))
                        .build();
                
                var response = openAiService.createChatCompletion(completionRequest);
                
                if (response.getChoices().isEmpty()) {
                    logger.error("No choices returned from OpenAI API");
                    throw new RuntimeException("No choices returned");
                }
                
                String jsonResponse = response.getChoices().get(0).getMessage().getContent();
                logger.debug("OpenAI budget response: {}", jsonResponse);
                
                // Clean the JSON response (remove code blocks if present)
                jsonResponse = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
                
                // Parse JSON response
                try {
                    BudgetPlanResponse budgetResponse = objectMapper.readValue(jsonResponse, BudgetPlanResponse.class);
                    // Validate and fix budget totals
                    return validateAndFixBudget(budgetResponse, request);
                } catch (Exception parseEx) {
                    logger.warn("Failed to parse JSON response, using fallback", parseEx);
                    throw new RuntimeException("Failed to parse response", parseEx);
                }
                
            } catch (Exception e) {
                logger.error("Error calling OpenAI budget API", e);
                throw new RuntimeException(e);
            }
        });
        
        try {
            return future.get(12, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("OpenAI budget request timed out after 12 seconds, using fallback");
            future.cancel(true);
            return createFallbackBudget(request);
        } catch (Exception e) {
            logger.error("Error in OpenAI budget request, using fallback", e);
            return createFallbackBudget(request);
        }
    }
    
    private String buildBudgetPrompt(BudgetRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a monthly budget plan. Details:\n");
        prompt.append("- Monthly income: $").append(request.getMonthlyIncome()).append("\n");
        prompt.append("- Fixed expenses: $").append(request.getFixedExpenses()).append("\n");
        prompt.append("- Savings rate target: ").append(request.getSavingsRate().multiply(BigDecimal.valueOf(100))).append("%\n");
        
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            prompt.append("- Categories to include: ").append(String.join(", ", request.getCategories())).append("\n");
        }
        
        prompt.append("\nReturn ONLY valid JSON matching this exact schema:\n");
        prompt.append("{\n");
        prompt.append("  \"monthlyIncome\": ").append(request.getMonthlyIncome()).append(",\n");
        prompt.append("  \"fixedExpenses\": ").append(request.getFixedExpenses()).append(",\n");
        prompt.append("  \"targetSavings\": ").append(request.getMonthlyIncome().multiply(request.getSavingsRate())).append(",\n");
        prompt.append("  \"variableTotal\": ").append(request.getMonthlyIncome().subtract(request.getFixedExpenses()).subtract(request.getMonthlyIncome().multiply(request.getSavingsRate()))).append(",\n");
        prompt.append("  \"allocations\": [\n");
        prompt.append("    {\"category\": \"string\", \"amount\": number, \"pctOfIncome\": number}\n");
        prompt.append("  ],\n");
        prompt.append("  \"recommendations\": [\"string\"]\n");
        prompt.append("}\n");
        prompt.append("\nEnsure fixed + sum(allocation amounts) + targetSavings = monthlyIncome");
        
        return prompt.toString();
    }
    
    private BudgetPlanResponse validateAndFixBudget(BudgetPlanResponse response, BudgetRequest request) {
        // Calculate expected values
        BigDecimal targetSavings = request.getMonthlyIncome().multiply(request.getSavingsRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal variableTotal = request.getMonthlyIncome().subtract(request.getFixedExpenses()).subtract(targetSavings).setScale(2, RoundingMode.HALF_UP);
        
        // Fix the response values
        response.setMonthlyIncome(request.getMonthlyIncome());
        response.setFixedExpenses(request.getFixedExpenses());
        response.setTargetSavings(targetSavings);
        response.setVariableTotal(variableTotal);
        
        // Proportionally adjust allocations to match variableTotal
        if (response.getAllocations() != null && !response.getAllocations().isEmpty()) {
            BigDecimal totalAllocated = response.getAllocations().stream()
                .map(Allocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalAllocated.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal scaleFactor = variableTotal.divide(totalAllocated, 4, RoundingMode.HALF_UP);
                
                for (Allocation allocation : response.getAllocations()) {
                    BigDecimal newAmount = allocation.getAmount().multiply(scaleFactor).setScale(2, RoundingMode.HALF_UP);
                    allocation.setAmount(newAmount);
                    allocation.setPctOfIncome(newAmount.divide(request.getMonthlyIncome(), 4, RoundingMode.HALF_UP));
                }
            }
        }
        
        return response;
    }
    
    private BudgetPlanResponse createFallbackBudget(BudgetRequest request) {
        BigDecimal targetSavings = request.getMonthlyIncome().multiply(request.getSavingsRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal variableTotal = request.getMonthlyIncome().subtract(request.getFixedExpenses()).subtract(targetSavings).setScale(2, RoundingMode.HALF_UP);
        
        List<Allocation> allocations = new ArrayList<>();
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            BigDecimal amountPerCategory = variableTotal.divide(BigDecimal.valueOf(request.getCategories().size()), 2, RoundingMode.HALF_UP);
            for (String category : request.getCategories()) {
                BigDecimal pctOfIncome = amountPerCategory.divide(request.getMonthlyIncome(), 4, RoundingMode.HALF_UP);
                allocations.add(new Allocation(category, amountPerCategory, pctOfIncome));
            }
        }
        
        List<String> recommendations = List.of(
            "Review and adjust allocations based on your actual spending patterns",
            "Automate your savings to ensure you meet your target",
            "Track expenses monthly to stay within budget"
        );
        
        return new BudgetPlanResponse(
            request.getMonthlyIncome(),
            request.getFixedExpenses(),
            targetSavings,
            variableTotal,
            allocations,
            recommendations
        );
    }
}