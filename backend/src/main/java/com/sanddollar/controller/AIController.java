package com.sanddollar.controller;

import com.sanddollar.dto.*;
import com.sanddollar.entity.User;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.OpenAIService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class AIController {

    @Autowired
    private OpenAIService openAIService;

    @PostMapping("/budget/chat")
    public ResponseEntity<?> budgetChat(
            @Valid @RequestBody BudgetChatRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            BudgetChatResponse response = openAIService.processBudgetChat(
                user, request.messages(), request.constraints());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to process budget chat: " + e.getMessage()));
        }
    }

    @PostMapping("/assistant/chat")
    public ResponseEntity<?> assistantChat(
            @Valid @RequestBody AssistantChatRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userPrincipal.getUser();
            AssistantChatResponse response = openAIService.processAssistantChat(
                user, request.messages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to process assistant chat: " + e.getMessage()));
        }
    }
}