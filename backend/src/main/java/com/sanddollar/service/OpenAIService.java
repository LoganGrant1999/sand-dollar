package com.sanddollar.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanddollar.dto.*;
import com.sanddollar.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    private static final String BUDGET_PLANNER_PROMPT = """
        You are Sand Dollar's budgeting planner. Provide educational guidance only (not financial advice).
        Rules:
        - Use server tools to read user spending; never invent data.
        - Propose a realistic plan that does not exceed historical net income.
        - If savings goals are aggressive, provide at least 3 trade-offs (e.g., trim dining out, swap groceries brands, prune subscriptions). Spread cuts across categories rather than a single deep cut.
        - Align period to user context (weekly/biweekly/monthly). Prefer starting next pay period unless the user requests immediate.
        - Output a compact JSON plan with categories, limits, and a savingsTargetCents. Keep category names human-friendly.
        """;

    private static final String DATA_ASSISTANT_PROMPT = """
        You are Sand Dollar's financial education assistant. You can summarize spending, identify cut opportunities, and sanity-check goals using read-only tools. Do not provide investment advice; frame as educational information.
        Always:
        - Cite concrete numbers from tool outputs (dates, amounts).
        - Offer options, trade-offs, and ranges.
        - Include a short disclaimer at the top: "This is educational information, not financial advice."
        """;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Autowired
    private AIToolService aiToolService;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    public BudgetChatResponse processBudgetChat(User user, List<ChatMessage> messages, BudgetConstraints constraints) {
        try {
            List<Map<String, Object>> openAIMessages = new ArrayList<>();
            openAIMessages.add(Map.of("role", "system", "content", BUDGET_PLANNER_PROMPT));
            
            for (ChatMessage msg : messages) {
                openAIMessages.add(Map.of("role", msg.role(), "content", msg.content()));
            }

            List<Map<String, Object>> tools = createBudgetTools();
            
            Map<String, Object> request = Map.of(
                "model", model,
                "messages", openAIMessages,
                "tools", tools,
                "tool_choice", "auto"
            );

            String response = callOpenAI(request);
            return processBudgetResponse(user, response, constraints);
            
        } catch (Exception e) {
            logger.error("Error processing budget chat", e);
            throw new RuntimeException("Failed to process budget chat: " + e.getMessage());
        }
    }

    public AssistantChatResponse processAssistantChat(User user, List<ChatMessage> messages) {
        try {
            List<Map<String, Object>> openAIMessages = new ArrayList<>();
            openAIMessages.add(Map.of("role", "system", "content", DATA_ASSISTANT_PROMPT));
            
            for (ChatMessage msg : messages) {
                openAIMessages.add(Map.of("role", msg.role(), "content", msg.content()));
            }

            List<Map<String, Object>> tools = createAssistantTools();
            
            Map<String, Object> request = Map.of(
                "model", model,
                "messages", openAIMessages,
                "tools", tools,
                "tool_choice", "auto"
            );

            String response = callOpenAI(request);
            return processAssistantResponse(user, response);
            
        } catch (Exception e) {
            logger.error("Error processing assistant chat", e);
            throw new RuntimeException("Failed to process assistant chat: " + e.getMessage());
        }
    }

    private String callOpenAI(Map<String, Object> request) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String requestBody = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "https://api.openai.com/v1/chat/completions", entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("OpenAI API call failed: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private BudgetChatResponse processBudgetResponse(User user, String response, BudgetConstraints constraints) 
            throws JsonProcessingException {
        JsonNode jsonResponse = objectMapper.readTree(response);
        JsonNode choice = jsonResponse.get("choices").get(0);
        JsonNode message = choice.get("message");

        // Handle tool calls if present
        if (message.has("tool_calls")) {
            return processToolCalls(user, jsonResponse, constraints, true);
        }

        // Extract final response
        String content = message.get("content").asText();
        return new BudgetChatResponse(content, null);
    }

    private AssistantChatResponse processAssistantResponse(User user, String response) 
            throws JsonProcessingException {
        JsonNode jsonResponse = objectMapper.readTree(response);
        JsonNode choice = jsonResponse.get("choices").get(0);
        JsonNode message = choice.get("message");

        // Handle tool calls if present
        if (message.has("tool_calls")) {
            BudgetChatResponse toolResponse = processToolCalls(user, jsonResponse, null, false);
            return new AssistantChatResponse(toolResponse.summaryText());
        }

        // Extract final response
        String content = message.get("content").asText();
        String disclaimer = "This is educational information, not financial advice.\n\n";
        return new AssistantChatResponse(disclaimer + content);
    }

    private BudgetChatResponse processToolCalls(User user, JsonNode response, 
                                              BudgetConstraints constraints, boolean isBudgeting) 
            throws JsonProcessingException {
        JsonNode toolCalls = response.get("choices").get(0).get("message").get("tool_calls");
        
        List<Map<String, Object>> toolResults = new ArrayList<>();
        Object budgetPlan = null;

        for (JsonNode toolCall : toolCalls) {
            String toolName = toolCall.get("function").get("name").asText();
            String arguments = toolCall.get("function").get("arguments").asText();
            String toolCallId = toolCall.get("id").asText();

            Map<String, Object> args = objectMapper.readValue(arguments, Map.class);
            Object result = executeTool(user, toolName, args, constraints);

            if ("propose_budget_plan".equals(toolName) && result != null) {
                budgetPlan = result;
            }

            toolResults.add(Map.of(
                "tool_call_id", toolCallId,
                "role", "tool",
                "content", objectMapper.writeValueAsString(result)
            ));
        }

        // For simplicity in this implementation, return the tool results
        // In a full implementation, you'd make another OpenAI call with the tool results
        String summary = isBudgeting ? 
            "Budget plan created successfully based on your spending data and goals." :
            "Analysis completed based on your financial data.";

        return new BudgetChatResponse(summary, budgetPlan);
    }

    private Object executeTool(User user, String toolName, Map<String, Object> args, BudgetConstraints constraints) {
        return switch (toolName) {
            case "get_user_spend_summary" -> aiToolService.getUserSpendSummary(user, (String) args.get("period"));
            case "propose_budget_plan" -> aiToolService.proposeBudgetPlan(user, args, constraints);
            case "get_balances" -> aiToolService.getBalances(user);
            case "get_transactions" -> aiToolService.getTransactions(user, args);
            case "get_active_budget" -> aiToolService.getActiveBudget(user);
            case "get_goal_progress" -> aiToolService.getGoalProgress(user);
            default -> Map.of("error", "Unknown tool: " + toolName);
        };
    }

    private List<Map<String, Object>> createBudgetTools() {
        return Arrays.asList(
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "get_user_spend_summary",
                    "description", "Get spending summary by category for a period",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "period", Map.of(
                                "type", "string",
                                "enum", Arrays.asList("30d", "60d", "90d"),
                                "description", "Time period for analysis"
                            )
                        ),
                        "required", Arrays.asList("period")
                    )
                )
            ),
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "propose_budget_plan",
                    "description", "Create and save a budget plan based on goals and constraints",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "goals", Map.of(
                                "type", "object",
                                "description", "User goals with targetCents, targetDate, notes"
                            ),
                            "summary", Map.of(
                                "type", "object",
                                "description", "Spending summary data"
                            ),
                            "constraints", Map.of(
                                "type", "object",
                                "description", "Budget constraints like period, startDate"
                            )
                        ),
                        "required", Arrays.asList("goals")
                    )
                )
            )
        );
    }

    private List<Map<String, Object>> createAssistantTools() {
        return Arrays.asList(
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "get_balances",
                    "description", "Get current account balances",
                    "parameters", Map.of("type", "object", "properties", Map.of())
                )
            ),
            Map.of(
                "type", "function", 
                "function", Map.of(
                    "name", "get_transactions",
                    "description", "Get transactions for a date range",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "range", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "startDate", Map.of("type", "string"),
                                    "endDate", Map.of("type", "string")
                                )
                            )
                        ),
                        "required", Arrays.asList("range")
                    )
                )
            ),
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "get_active_budget", 
                    "description", "Get the active budget plan",
                    "parameters", Map.of("type", "object", "properties", Map.of())
                )
            ),
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "get_goal_progress",
                    "description", "Get progress on financial goals",
                    "parameters", Map.of("type", "object", "properties", Map.of())
                )
            )
        );
    }
}