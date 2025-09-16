package com.sanddollar.service;

import com.sanddollar.dto.*;
import com.sanddollar.entity.Budget;
import com.sanddollar.entity.BudgetAllocation;
import com.sanddollar.repository.BudgetRepository;
import com.sanddollar.repository.BudgetAllocationRepository;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BudgetAdjustmentService {
    private static final Logger logger = LoggerFactory.getLogger(BudgetAdjustmentService.class);
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private BudgetAllocationRepository budgetAllocationRepository;
    
    @Autowired
    private OpenAIService openAIService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BudgetAdjustmentResponse adjustBudget(BudgetAdjustmentRequest request) {
        try {
            logger.info("Processing budget adjustment request: {}", request.getInstruction());
            
            // Get current budget (or for specified scope)
            Budget currentBudget = getCurrentBudget(request.getScope());
            if (currentBudget == null) {
                return new BudgetAdjustmentResponse("error", "No active budget found. Please create a budget first using the budget wizard.");
            }
            
            // Parse the instruction
            ParsedBudgetChange parsedChange = parseInstruction(request.getInstruction());
            
            // Calculate the proposed changes
            List<BudgetAdjustmentResponse.BudgetDiff> diffs = calculateDiffs(currentBudget, parsedChange);
            
            // Check if zero-based balance would be preserved
            BigDecimal totalDelta = diffs.stream()
                .map(BudgetAdjustmentResponse.BudgetDiff::getDeltaAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // If confirm=true, apply the changes
            if (Boolean.TRUE.equals(request.getConfirm())) {
                if (totalDelta.compareTo(BigDecimal.ZERO) != 0 && request.getSourceCategory() == null) {
                    throw new RuntimeException("Cannot confirm changes without specifying source category for balance");
                }
                
                return applyChanges(currentBudget, diffs, request.getSourceCategory());
            }
            
            // If balance is preserved, return success
            if (totalDelta.compareTo(BigDecimal.ZERO) == 0) {
                BudgetAdjustmentResponse response = new BudgetAdjustmentResponse("success");
                BudgetAdjustmentResponse.BudgetAdjustmentProposal proposal = new BudgetAdjustmentResponse.BudgetAdjustmentProposal(diffs);
                response.setProposal(proposal);
                return response;
            }
            
            // If balance would break and no source provided, return confirmation needed
            if (request.getSourceCategory() == null) {
                BudgetAdjustmentResponse response = new BudgetAdjustmentResponse("needs_confirmation");
                BudgetAdjustmentResponse.BudgetAdjustmentProposal proposal = new BudgetAdjustmentResponse.BudgetAdjustmentProposal(diffs);
                response.setProposal(proposal);
                response.setOptions(generateSourceOptions(currentBudget, totalDelta.abs()));
                return response;
            }
            
            // If source category provided, add it to diffs and return success
            diffs.add(createSourceCategoryDiff(currentBudget, request.getSourceCategory(), totalDelta.negate()));
            BudgetAdjustmentResponse response = new BudgetAdjustmentResponse("success");
            BudgetAdjustmentResponse.BudgetAdjustmentProposal proposal = new BudgetAdjustmentResponse.BudgetAdjustmentProposal(diffs);
            response.setProposal(proposal);
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing budget adjustment", e);
            throw new RuntimeException("Failed to process budget adjustment: " + e.getMessage());
        }
    }
    
    private Budget getCurrentBudget(BudgetAdjustmentRequest.BudgetScope scope) {
        Budget budget = null;
        if (scope != null && scope.getMonth() != null && scope.getYear() != null) {
            return budgetRepository.findByUserIdAndMonthAndYear(1L, scope.getMonth(), scope.getYear())
                .orElse(null);
        }
        
        // Get current active budget
        return budgetRepository.findCurrentBudgetByUserId(1L).orElse(null);
    }
    
    private ParsedBudgetChange parseInstruction(String instruction) {
        // Try simple pattern matching first
        ParsedBudgetChange simpleResult = parseSimplePatterns(instruction);
        if (simpleResult != null) {
            return simpleResult;
        }
        
        // Fall back to GPT-5-mini for complex instructions
        return parseWithGPT(instruction);
    }
    
    private ParsedBudgetChange parseSimplePatterns(String instruction) {
        String normalized = instruction.toLowerCase().trim();
        
        // Pattern: "increase/decrease [category] by [amount/percentage]"
        Pattern pattern = Pattern.compile("(increase|decrease|reduce)\\s+([\\w\\s]+?)\\s+by\\s+(\\$?([0-9]+(?:\\.[0-9]+)?)([%]?))", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(normalized);
        
        if (matcher.find()) {
            String action = matcher.group(1);
            String category = matcher.group(2).trim();
            String amountStr = matcher.group(4);
            boolean isPercentage = matcher.group(5) != null;
            
            BigDecimal value = new BigDecimal(amountStr);
            if ("decrease".equals(action) || "reduce".equals(action)) {
                value = value.negate();
            }
            
            ParsedBudgetChange.CategoryChange change;
            if (isPercentage) {
                change = new ParsedBudgetChange.CategoryChange(category, null, value);
            } else {
                change = new ParsedBudgetChange.CategoryChange(category, value.multiply(new BigDecimal("100")), null); // Convert to cents
            }
            
            return new ParsedBudgetChange(List.of(change), null);
        }
        
        // Pattern: "move [amount] from [source] to [target]"
        Pattern movePattern = Pattern.compile("move\\s+\\$?([0-9]+(?:\\.[0-9]+)?)\\s+from\\s+([\\w\\s]+?)\\s+to\\s+([\\w\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher moveMatcher = movePattern.matcher(normalized);
        
        if (moveMatcher.find()) {
            BigDecimal amount = new BigDecimal(moveMatcher.group(1)).multiply(new BigDecimal("100")); // Convert to cents
            String sourceCategory = moveMatcher.group(2).trim();
            String targetCategory = moveMatcher.group(3).trim();
            
            List<ParsedBudgetChange.CategoryChange> changes = List.of(
                new ParsedBudgetChange.CategoryChange(targetCategory, amount, null)
            );
            
            return new ParsedBudgetChange(changes, sourceCategory);
        }
        
        return null;
    }
    
    private ParsedBudgetChange parseWithGPT(String instruction) {
        try {
            String prompt = "Parse this budget adjustment instruction into JSON. Return ONLY valid JSON matching this schema:\n" +
                "{\n" +
                "  \"changes\": [{\"category\": \"string\", \"deltaAmount\": number_in_cents_or_null, \"deltaPercent\": number_or_null}],\n" +
                "  \"sourceCategory\": \"string_or_null\"\n" +
                "}\n\n" +
                "Instruction: \"" + instruction + "\"\n\n" +
                "Notes:\n" +
                "- Use deltaAmount for dollar amounts (in cents, so $10 = 1000)\n" +
                "- Use deltaPercent for percentage changes (15% = 15)\n" +
                "- Positive values = increase, negative = decrease\n" +
                "- sourceCategory only if explicitly mentioned as source of funds";
            
            List<ChatMessage> messages = List.of(
                new ChatMessage("system", "You are a budget parsing assistant. Return only valid JSON."),
                new ChatMessage("user", prompt)
            );
            
            String response = openAIService.chatOnce(messages, 0.1);
            
            // Clean the response
            response = response.replaceAll("```json", "").replaceAll("```", "").trim();
            
            return objectMapper.readValue(response, ParsedBudgetChange.class);
            
        } catch (Exception e) {
            logger.warn("Failed to parse with GPT, using fallback", e);
            // Fallback: try to extract category name and treat as simple increase
            String category = extractCategoryName(instruction);
            if (category != null) {
                ParsedBudgetChange.CategoryChange change = new ParsedBudgetChange.CategoryChange(category, new BigDecimal("1000"), null); // $10 increase
                return new ParsedBudgetChange(List.of(change), null);
            }
            throw new RuntimeException("Could not parse instruction: " + instruction);
        }
    }
    
    private String extractCategoryName(String instruction) {
        // Try to extract common category names
        String[] commonCategories = {"entertainment", "groceries", "dining", "transportation", "utilities", "insurance", "shopping", "gas", "food"};
        String normalized = instruction.toLowerCase();
        
        for (String category : commonCategories) {
            if (normalized.contains(category)) {
                return category;
            }
        }
        return null;
    }
    
    private List<BudgetAdjustmentResponse.BudgetDiff> calculateDiffs(Budget budget, ParsedBudgetChange parsedChange) {
        List<BudgetAllocation> allocations = budgetAllocationRepository.findByBudgetIdOrderByTypeAscCategoryAsc(budget.getId());
        Map<String, BudgetAllocation> allocationMap = allocations.stream()
            .collect(Collectors.toMap(
                a -> a.getCategory().toLowerCase(),
                a -> a,
                (existing, replacement) -> existing
            ));
        
        List<BudgetAdjustmentResponse.BudgetDiff> diffs = new ArrayList<>();
        
        for (ParsedBudgetChange.CategoryChange change : parsedChange.getChanges()) {
            String categoryKey = change.getCategory().toLowerCase();
            BudgetAllocation allocation = allocationMap.get(categoryKey);
            
            if (allocation == null) {
                // Try fuzzy matching
                allocation = findBestMatchingCategory(categoryKey, allocations);
            }
            
            if (allocation == null) {
                throw new RuntimeException("Category '" + change.getCategory() + "' not found in your current budget. Available categories: " + 
                    allocations.stream().map(a -> a.getCategory()).collect(Collectors.joining(", ")));
            }
            
            BigDecimal currentAmount = allocation.getAmount();
            BigDecimal deltaAmount;
            
            if (change.getDeltaAmount() != null) {
                deltaAmount = change.getDeltaAmount();
            } else if (change.getDeltaPercent() != null) {
                deltaAmount = currentAmount.multiply(change.getDeltaPercent())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                throw new RuntimeException("Either deltaAmount or deltaPercent must be specified");
            }
            
            BigDecimal newAmount = currentAmount.add(deltaAmount);
            if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Budget cannot be negative for category: " + allocation.getCategory());
            }
            
            diffs.add(new BudgetAdjustmentResponse.BudgetDiff(
                allocation.getCategory(),
                currentAmount,
                newAmount,
                deltaAmount
            ));
        }
        
        return diffs;
    }
    
    private BudgetAllocation findBestMatchingCategory(String input, List<BudgetAllocation> allocations) {
        String inputLower = input.toLowerCase();
        
        // Exact match first
        for (BudgetAllocation allocation : allocations) {
            if (allocation.getCategory().toLowerCase().equals(inputLower)) {
                return allocation;
            }
        }
        
        // Partial match
        for (BudgetAllocation allocation : allocations) {
            String categoryLower = allocation.getCategory().toLowerCase();
            if (categoryLower.contains(inputLower) || inputLower.contains(categoryLower)) {
                return allocation;
            }
        }
        
        return null;
    }
    
    private List<BudgetAdjustmentResponse.SourceCategoryOption> generateSourceOptions(Budget budget, BigDecimal neededAmount) {
        List<BudgetAllocation> allocations = budgetAllocationRepository.findByBudgetIdOrderByTypeAscCategoryAsc(budget.getId());
        
        return allocations.stream()
            .filter(a -> a.getAmount().compareTo(neededAmount) >= 0) // Can cover the full amount
            .sorted((a, b) -> b.getAmount().compareTo(a.getAmount())) // Largest first
            .limit(3)
            .map(a -> new BudgetAdjustmentResponse.SourceCategoryOption(
                a.getCategory(),
                a.getAmount(),
                neededAmount
            ))
            .collect(Collectors.toList());
    }
    
    private BudgetAdjustmentResponse.BudgetDiff createSourceCategoryDiff(Budget budget, String sourceCategory, BigDecimal deltaAmount) {
        List<BudgetAllocation> allocations = budgetAllocationRepository.findByBudgetIdOrderByTypeAscCategoryAsc(budget.getId());
        BudgetAllocation sourceAllocation = findBestMatchingCategory(sourceCategory.toLowerCase(), allocations);
        
        if (sourceAllocation == null) {
            throw new RuntimeException("Source category not found: " + sourceCategory);
        }
        
        BigDecimal currentAmount = sourceAllocation.getAmount();
        BigDecimal newAmount = currentAmount.add(deltaAmount);
        
        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient funds in source category: " + sourceCategory);
        }
        
        return new BudgetAdjustmentResponse.BudgetDiff(
            sourceAllocation.getCategory(),
            currentAmount,
            newAmount,
            deltaAmount
        );
    }
    
    @Transactional
    private BudgetAdjustmentResponse applyChanges(Budget budget, List<BudgetAdjustmentResponse.BudgetDiff> diffs, String sourceCategory) {
        try {
            // Apply all changes
            for (BudgetAdjustmentResponse.BudgetDiff diff : diffs) {
                List<BudgetAllocation> allocations = budgetAllocationRepository.findByBudgetIdOrderByTypeAscCategoryAsc(budget.getId());
                BudgetAllocation allocation = allocations.stream()
                    .filter(a -> a.getCategory().equalsIgnoreCase(diff.getCategory()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Category not found: " + diff.getCategory()));
                
                allocation.setAmount(diff.getNewAmount());
                budgetAllocationRepository.save(allocation);
            }
            
            // If source category was specified, apply that change too
            if (sourceCategory != null) {
                BigDecimal totalDelta = diffs.stream()
                    .map(BudgetAdjustmentResponse.BudgetDiff::getDeltaAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (totalDelta.compareTo(BigDecimal.ZERO) != 0) {
                    BudgetAdjustmentResponse.BudgetDiff sourceDiff = createSourceCategoryDiff(budget, sourceCategory, totalDelta.negate());
                    
                    List<BudgetAllocation> allocations = budgetAllocationRepository.findByBudgetIdOrderByTypeAscCategoryAsc(budget.getId());
                    BudgetAllocation sourceAllocation = allocations.stream()
                        .filter(a -> a.getCategory().equalsIgnoreCase(sourceDiff.getCategory()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Source category not found: " + sourceCategory));
                    
                    sourceAllocation.setAmount(sourceDiff.getNewAmount());
                    budgetAllocationRepository.save(sourceAllocation);
                    
                    diffs.add(sourceDiff);
                }
            }
            
            // Return success with updated budget
            BudgetAdjustmentResponse response = new BudgetAdjustmentResponse("success");
            response.setUpdatedBudget(budget); // In a real app, you'd return the full budget with allocations
            BudgetAdjustmentResponse.BudgetAdjustmentProposal proposal = new BudgetAdjustmentResponse.BudgetAdjustmentProposal(diffs);
            response.setProposal(proposal);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error applying budget changes", e);
            throw new RuntimeException("Failed to apply changes: " + e.getMessage());
        }
    }
}