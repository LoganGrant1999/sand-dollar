package com.sanddollar.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAiClient.class);
    
    @Value("${openai.api-key:}")
    private String apiKey;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String model;
    
    @Value("${openai.max-tokens:2000}")
    private Integer maxTokens;
    
    @Value("${openai.temperature:0.3}")
    private Double temperature;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OpenAiClient() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("Authorization", "Bearer " + apiKey);
            request.getHeaders().set("Content-Type", "application/json");
            return execution.execute(request, body);
        });
        
        // Set 15 second timeout
        this.restTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory());
        ((org.springframework.http.client.SimpleClientHttpRequestFactory) this.restTemplate.getRequestFactory())
            .setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
        ((org.springframework.http.client.SimpleClientHttpRequestFactory) this.restTemplate.getRequestFactory())
            .setReadTimeout((int) Duration.ofSeconds(15).toMillis());
        
        this.objectMapper = new ObjectMapper();
    }
    
    public OpenAiResponse generateBudgetRecommendations(String systemPrompt, String userPrompt) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("OpenAI API key not configured, using fallback");
                throw new RuntimeException("OpenAI API key not configured");
            }
            
            OpenAiRequest request = new OpenAiRequest(
                model,
                List.of(
                    new OpenAiMessage("system", systemPrompt),
                    new OpenAiMessage("user", userPrompt)
                ),
                maxTokens,
                temperature,
                0.1  // top_p for more focused responses
            );
            
            logger.info("Calling OpenAI API with model: {}", model);
            
            ResponseEntity<OpenAiResponse> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                request,
                OpenAiResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("OpenAI API call successful. Tokens used: prompt={}, completion={}", 
                    response.getBody().usage.promptTokens,
                    response.getBody().usage.completionTokens);
                return response.getBody();
            } else {
                throw new RuntimeException("OpenAI API returned unexpected response: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("OpenAI API call failed", e);
            throw e;
        }
    }
    
    // Request/Response DTOs for OpenAI API
    public static class OpenAiRequest {
        public String model;
        public List<OpenAiMessage> messages;
        @JsonProperty("max_tokens")
        public Integer maxTokens;
        public Double temperature;
        @JsonProperty("top_p")
        public Double topP;
        
        public OpenAiRequest(String model, List<OpenAiMessage> messages, Integer maxTokens, Double temperature, Double topP) {
            this.model = model;
            this.messages = messages;
            this.maxTokens = maxTokens;
            this.temperature = temperature;
            this.topP = topP;
        }
    }
    
    public static class OpenAiMessage {
        public String role;
        public String content;
        
        public OpenAiMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    
    public static class OpenAiResponse {
        public String id;
        public String object;
        public Long created;
        public String model;
        public List<OpenAiChoice> choices;
        public OpenAiUsage usage;
        
        public String getContent() {
            if (choices != null && !choices.isEmpty() && choices.get(0).message != null) {
                return choices.get(0).message.content;
            }
            return null;
        }
    }
    
    public static class OpenAiChoice {
        public Integer index;
        public OpenAiMessage message;
        @JsonProperty("finish_reason")
        public String finishReason;
    }
    
    public static class OpenAiUsage {
        @JsonProperty("prompt_tokens")
        public Integer promptTokens;
        @JsonProperty("completion_tokens")
        public Integer completionTokens;
        @JsonProperty("total_tokens")
        public Integer totalTokens;
    }
}