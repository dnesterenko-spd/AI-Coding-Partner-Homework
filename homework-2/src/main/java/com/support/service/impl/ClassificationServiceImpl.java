package com.support.service.impl;

import com.support.domain.Category;
import com.support.domain.ClassificationResult;
import com.support.domain.Priority;
import com.support.domain.Ticket;
import com.support.service.ClassificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClassificationServiceImpl implements ClassificationService {

    private static final Map<Category, Set<String>> CATEGORY_KEYWORDS = new HashMap<>();
    private static final Map<Priority, Set<String>> PRIORITY_KEYWORDS = new HashMap<>();

    static {
        // Initialize category keywords
        CATEGORY_KEYWORDS.put(Category.ACCOUNT_ACCESS, Set.of(
            "login", "password", "signin", "sign-in", "sign in", "2fa", "two-factor",
            "authentication", "authorize", "access", "locked out", "reset password",
            "forgot password", "can't login", "cannot login", "account locked"
        ));

        CATEGORY_KEYWORDS.put(Category.TECHNICAL_ISSUE, Set.of(
            "bug", "error", "crash", "not working", "broken", "issue", "problem",
            "fail", "failed", "failure", "exception", "500", "404", "timeout",
            "slow", "performance", "down", "outage", "unavailable"
        ));

        CATEGORY_KEYWORDS.put(Category.BILLING_QUESTION, Set.of(
            "payment", "invoice", "refund", "charge", "billing", "subscription",
            "credit card", "paypal", "price", "cost", "fee", "discount", "coupon",
            "trial", "upgrade", "downgrade", "cancel subscription"
        ));

        CATEGORY_KEYWORDS.put(Category.FEATURE_REQUEST, Set.of(
            "enhancement", "feature", "suggestion", "would be nice", "request",
            "improve", "add", "new feature", "functionality", "wish", "idea",
            "recommend", "could you", "it would be great"
        ));

        CATEGORY_KEYWORDS.put(Category.BUG_REPORT, Set.of(
            "defect", "reproduce", "steps to reproduce", "regression", "broken",
            "not as expected", "unexpected behavior", "malfunction", "glitch",
            "inconsistent", "incorrect", "wrong result"
        ));

        // Initialize priority keywords
        PRIORITY_KEYWORDS.put(Priority.URGENT, Set.of(
            "urgent", "critical", "emergency", "production down", "security",
            "data loss", "can't access", "completely broken", "affecting all users",
            "business critical", "asap", "immediately", "right now"
        ));

        PRIORITY_KEYWORDS.put(Priority.HIGH, Set.of(
            "important", "blocking", "high priority", "serious", "major",
            "significant impact", "need soon", "priority", "affecting many"
        ));

        PRIORITY_KEYWORDS.put(Priority.MEDIUM, Set.of(
            "moderate", "normal", "standard", "regular", "typical"
        ));

        PRIORITY_KEYWORDS.put(Priority.LOW, Set.of(
            "minor", "low priority", "nice to have", "when possible",
            "no rush", "whenever", "cosmetic", "trivial"
        ));
    }

    @Override
    public ClassificationResult classifyTicket(Ticket ticket) {
        return classifyTicket(ticket.getSubject(), ticket.getDescription());
    }

    @Override
    public ClassificationResult classifyTicket(String subject, String description) {
        String combinedText = (subject + " " + description).toLowerCase();

        // Find matching keywords for categories
        Map<Category, Integer> categoryScores = new HashMap<>();
        Map<Category, List<String>> categoryMatches = new HashMap<>();

        for (Map.Entry<Category, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            List<String> matches = findMatches(combinedText, entry.getValue());
            if (!matches.isEmpty()) {
                categoryScores.put(entry.getKey(), matches.size());
                categoryMatches.put(entry.getKey(), matches);
            }
        }

        // Find matching keywords for priorities
        Map<Priority, Integer> priorityScores = new HashMap<>();
        Map<Priority, List<String>> priorityMatches = new HashMap<>();

        for (Map.Entry<Priority, Set<String>> entry : PRIORITY_KEYWORDS.entrySet()) {
            List<String> matches = findMatches(combinedText, entry.getValue());
            if (!matches.isEmpty()) {
                priorityScores.put(entry.getKey(), matches.size());
                priorityMatches.put(entry.getKey(), matches);
            }
        }

        // Determine category with highest score
        Category selectedCategory = categoryScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(Category.OTHER);

        // Determine priority (with urgency bias)
        Priority selectedPriority;
        if (priorityScores.containsKey(Priority.URGENT)) {
            selectedPriority = Priority.URGENT;
        } else if (priorityScores.containsKey(Priority.HIGH)) {
            selectedPriority = Priority.HIGH;
        } else if (priorityScores.containsKey(Priority.LOW)) {
            selectedPriority = Priority.LOW;
        } else {
            selectedPriority = Priority.MEDIUM; // Default priority
        }

        // Calculate confidence score
        double categoryConfidence = selectedCategory == Category.OTHER ? 0.0 :
            Math.min(1.0, categoryScores.get(selectedCategory) * 0.25);
        double priorityConfidence = priorityScores.isEmpty() ? 0.5 :
            Math.min(1.0, priorityScores.getOrDefault(selectedPriority, 0) * 0.3);
        double overallConfidence = (categoryConfidence + priorityConfidence) / 2;

        // Collect all matched keywords
        List<String> allMatchedKeywords = new ArrayList<>();
        if (categoryMatches.containsKey(selectedCategory)) {
            allMatchedKeywords.addAll(categoryMatches.get(selectedCategory));
        }
        if (priorityMatches.containsKey(selectedPriority)) {
            allMatchedKeywords.addAll(priorityMatches.get(selectedPriority));
        }

        // Build reasoning
        String reasoning = buildReasoning(selectedCategory, selectedPriority,
                                         allMatchedKeywords, overallConfidence);

        return ClassificationResult.builder()
            .category(selectedCategory)
            .priority(selectedPriority)
            .confidenceScore(overallConfidence)
            .matchedKeywords(allMatchedKeywords)
            .reasoning(reasoning)
            .classifiedAt(LocalDateTime.now())
            .isManualOverride(false)
            .build();
    }

    @Override
    public void reclassifyTicket(Ticket ticket) {
        ClassificationResult newClassification = classifyTicket(ticket);
        ticket.setClassification(newClassification);

        // Update ticket category and priority
        ticket.setCategory(newClassification.getCategory());
        ticket.setPriority(newClassification.getPriority());

        log.info("Reclassified ticket {} - Category: {}, Priority: {}, Confidence: {}",
                ticket.getId(), newClassification.getCategory(),
                newClassification.getPriority(), newClassification.getConfidenceScore());
    }

    @Override
    public ClassificationResult manualOverride(Ticket ticket, ClassificationResult override) {
        override.setIsManualOverride(true);
        override.setClassifiedAt(LocalDateTime.now());

        if (override.getReasoning() == null || override.getReasoning().isEmpty()) {
            override.setReasoning("Manual classification override by user");
        }

        ticket.setClassification(override);
        ticket.setCategory(override.getCategory());
        ticket.setPriority(override.getPriority());

        log.info("Manual override for ticket {} - Category: {}, Priority: {}",
                ticket.getId(), override.getCategory(), override.getPriority());

        return override;
    }

    private List<String> findMatches(String text, Set<String> keywords) {
        return keywords.stream()
            .filter(keyword -> text.contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
    }

    private String buildReasoning(Category category, Priority priority,
                                 List<String> matchedKeywords, double confidence) {
        StringBuilder reasoning = new StringBuilder();

        if (category == Category.OTHER && matchedKeywords.isEmpty()) {
            reasoning.append("No specific keywords matched. Defaulting to 'Other' category.");
        } else {
            reasoning.append("Classified as '").append(category.getDisplayName())
                   .append("' based on keywords: ")
                   .append(matchedKeywords.stream().limit(5).collect(Collectors.joining(", ")));
        }

        reasoning.append(" Priority set to '").append(priority.getDisplayName()).append("'");

        if (confidence < 0.5) {
            reasoning.append(" (Low confidence - manual review recommended)");
        } else if (confidence < 0.7) {
            reasoning.append(" (Medium confidence)");
        } else {
            reasoning.append(" (High confidence)");
        }

        return reasoning.toString();
    }
}