package com.sanddollar.controller;

import com.sanddollar.dto.BudgetRequest;
import com.sanddollar.dto.BudgetPlanResponse;
import com.sanddollar.dto.ChatRequest;
import com.sanddollar.dto.BudgetAdjustmentRequest;
import com.sanddollar.dto.BudgetAdjustmentResponse;
import com.sanddollar.service.OpenAIService;
import com.sanddollar.service.BudgetAdjustmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/ai")
public class AIController {
    private static final Logger logger = LoggerFactory.getLogger(AIController.class);
    
    @Autowired
    private OpenAIService openAIService;
    
    @Autowired
    private BudgetAdjustmentService budgetAdjustmentService;
    
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            logger.info("Received chat request with prompt: {}", prompt);
            String response = openAIService.chatOnce(prompt);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain")
                    .body(response);
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String prompt) {
        logger.info("Received streaming chat request with prompt: {}", prompt);
        
        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
        
        try {
            openAIService.chatStream(prompt, token -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("data")
                        .data(token));
                } catch (IOException e) {
                    logger.error("Error sending SSE data", e);
                    emitter.completeWithError(e);
                }
            }, () -> {
                // Complete the emitter when streaming is done
                emitter.complete();
            });
            
        } catch (Exception e) {
            logger.error("Error processing streaming chat request", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    @PostMapping("/budget/plan")
    public ResponseEntity<BudgetPlanResponse> generateBudgetPlan(@Valid @RequestBody BudgetRequest request) {
        try {
            logger.info("Received budget plan request: {}", request);
            BudgetPlanResponse response = openAIService.generateBudget(request);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(response);
        } catch (Exception e) {
            logger.error("Error processing budget plan request", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/chat/answer")
    public ResponseEntity<?> chatAnswer(@Valid @RequestBody ChatRequest request,
                                       @RequestHeader(value = "Accept", defaultValue = "application/json") String acceptHeader) {
        try {
            logger.info("Received chat answer request with {} messages", request.getMessages().size());
            
            // Add SandDollar system prompt if not present
            var originalMessages = request.toOpenAIChatMessages();
            var messages = new java.util.ArrayList<>(originalMessages);
            boolean hasSystemMessage = messages.stream().anyMatch(msg -> "system".equals(msg.getRole()));
            
            if (!hasSystemMessage) {
                messages.add(0, new com.theokanning.openai.completion.chat.ChatMessage("system", 
                    "You are SandDollar's budgeting assistant. Be concise, practical, numerate. " +
                    "When asked to change budgets, call /api/budgets/adjust (see below) and then confirm deltas. " +
                    "Focus on actionable financial advice and budget optimization."));
            }
            
            // Check if streaming is requested
            if (acceptHeader.contains("text/event-stream")) {
                return handleStreamingResponse(messages, request.getTemperature());
            } else {
                // Non-streaming response
                String response = openAIService.chatOnce(messages, request.getTemperature());
                return ResponseEntity.ok()
                        .header("Content-Type", "text/plain")
                        .body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing chat answer request", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    private ResponseEntity<SseEmitter> handleStreamingResponse(java.util.List<com.theokanning.openai.completion.chat.ChatMessage> messages, 
                                                              Double temperature) {
        logger.info("Handling streaming chat response");
        
        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
        
        try {
            openAIService.chatStream(messages, temperature, token -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("data")
                        .data(token));
                } catch (IOException e) {
                    logger.error("Error sending SSE data", e);
                    emitter.completeWithError(e);
                }
            }, () -> {
                // Complete the emitter when streaming is done
                emitter.complete();
            });
            
        } catch (Exception e) {
            logger.error("Error processing streaming chat answer request", e);
            emitter.completeWithError(e);
        }
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(emitter);
    }
    
    @PostMapping("/budget/adjust")
    public ResponseEntity<Map<String, Object>> adjustBudgetWithAI(@RequestBody BudgetAdjustmentRequest request) {
        try {
            logger.info("Received AI budget adjustment request: {}", request.getInstruction());
            
            // Call the budget adjustment service
            BudgetAdjustmentResponse adjustmentResponse = budgetAdjustmentService.adjustBudget(request);
            
            // Generate a friendly confirmation message
            String confirmationMessage = generateConfirmationMessage(adjustmentResponse, request.getInstruction());
            
            // Return both the friendly message and the adjustment data
            Map<String, Object> response = new HashMap<>();
            response.put("message", confirmationMessage);
            response.put("adjustmentData", adjustmentResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing AI budget adjustment", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "I encountered an error while trying to adjust your budget: " + e.getMessage());
            errorResponse.put("adjustmentData", null);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    private String generateConfirmationMessage(BudgetAdjustmentResponse response, String instruction) {
        if ("success".equals(response.getStatus())) {
            return "✅ I've successfully updated your budget based on your request: \"" + instruction + "\". " +
                   "The changes have been applied and your zero-based budget balance is maintained.";
        } else if ("needs_confirmation".equals(response.getStatus())) {
            return "I understand you want to: \"" + instruction + "\". However, this change would affect your budget balance. " +
                   "I've identified some categories you could reduce to cover this adjustment. Please review the options and confirm.";
        } else {
            return "I couldn't process your budget adjustment request: \"" + instruction + "\". Please try rephrasing or provide more specific details.";
        }
    }
}