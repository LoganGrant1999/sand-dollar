package com.sanddollar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.util.List;

public class ChatRequest {
    @NotNull
    @Size(min = 1, max = 50)
    @Valid
    private List<ChatMessage> messages;
    
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "2.0")
    private Double temperature;
    
    public ChatRequest() {}
    
    public ChatRequest(List<ChatMessage> messages, Double temperature) {
        this.messages = messages;
        this.temperature = temperature;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public List<com.theokanning.openai.completion.chat.ChatMessage> toOpenAIChatMessages() {
        return messages.stream()
            .map(msg -> new com.theokanning.openai.completion.chat.ChatMessage(msg.role(), msg.content()))
            .toList();
    }
}