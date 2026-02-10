package com.support.service;

import com.support.domain.Category;
import com.support.domain.ClassificationResult;
import com.support.domain.Priority;
import com.support.domain.Ticket;
import com.support.service.impl.ClassificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Classification Service Tests")
class ClassificationServiceTest {

    private ClassificationServiceImpl classificationService;

    @BeforeEach
    void setUp() {
        classificationService = new ClassificationServiceImpl();
    }

    @Test
    @DisplayName("Classify - Account Access category from login keyword")
    void testClassifyAccountAccess() {
        ClassificationResult result = classificationService.classifyTicket(
                "Cannot login to account",
                "I am unable to login to my account with my password");

        assertEquals(Category.ACCOUNT_ACCESS, result.getCategory());
        assertTrue(result.getMatchedKeywords().contains("login"));
        assertFalse(result.getMatchedKeywords().isEmpty());
    }

    @Test
    @DisplayName("Classify - Technical Issue category from error keyword")
    void testClassifyTechnicalIssue() {
        ClassificationResult result = classificationService.classifyTicket(
                "Application error occurred",
                "Getting error 500 when accessing the system. The crash happens consistently.");

        assertEquals(Category.TECHNICAL_ISSUE, result.getCategory());
        assertTrue(result.getMatchedKeywords().contains("error"));
    }

    @Test
    @DisplayName("Classify - Billing category from payment keyword")
    void testClassifyBilling() {
        ClassificationResult result = classificationService.classifyTicket(
                "Invoice not received",
                "I was charged twice for my subscription. Need refund on duplicate charge.");

        assertEquals(Category.BILLING_QUESTION, result.getCategory());
        assertTrue(result.getMatchedKeywords().size() > 0);
    }

    @Test
    @DisplayName("Classify - Feature Request category from suggestion keyword")
    void testClassifyFeatureRequest() {
        ClassificationResult result = classificationService.classifyTicket(
                "Enhancement request",
                "It would be great to have dark mode as a feature in the application.");

        assertEquals(Category.FEATURE_REQUEST, result.getCategory());
        assertTrue(result.getMatchedKeywords().contains("feature"));
    }

    @Test
    @DisplayName("Classify - Bug Report category from defect keyword")
    void testClassifyBugReport() {
        ClassificationResult result = classificationService.classifyTicket(
                "Defect in checkout",
                "Steps to reproduce: 1) Go to checkout 2) Apply discount code 3) Get error");

        assertEquals(Category.BUG_REPORT, result.getCategory());
        assertTrue(result.getMatchedKeywords().size() > 0);
    }

    @Test
    @DisplayName("Priority - URGENT from production keyword")
    void testPriorityUrgent() {
        ClassificationResult result = classificationService.classifyTicket(
                "Critical issue",
                "Production is down and security is at risk. Immediate action needed.");

        assertEquals(Priority.URGENT, result.getPriority());
        assertTrue(result.getConfidenceScore() > 0.5);
    }

    @Test
    @DisplayName("Priority - HIGH from important keyword")
    void testPriorityHigh() {
        ClassificationResult result = classificationService.classifyTicket(
                "Important blocking issue",
                "This is important and blocking our team's work. Need a fix soon.");

        assertEquals(Priority.HIGH, result.getPriority());
    }

    @Test
    @DisplayName("Priority - MEDIUM for neutral keywords")
    void testPriorityMedium() {
        ClassificationResult result = classificationService.classifyTicket(
                "Standard support request",
                "This is a normal request for standard features");

        assertEquals(Priority.MEDIUM, result.getPriority());
    }

    @Test
    @DisplayName("Priority - LOW from low priority keyword")
    void testPriorityLow() {
        ClassificationResult result = classificationService.classifyTicket(
                "Minor cosmetic issue",
                "This is nice to have when possible and trivial to implement");

        assertEquals(Priority.LOW, result.getPriority());
    }

    @Test
    @DisplayName("Confidence score - Should be between 0 and 1")
    void testConfidenceScore() {
        ClassificationResult result = classificationService.classifyTicket(
                "Test subject",
                "Test description with minimum length");

        assertTrue(result.getConfidenceScore() >= 0.0);
        assertTrue(result.getConfidenceScore() <= 1.0);
    }

    @Test
    @DisplayName("Classification result - Should include reasoning")
    void testReasoningGenerated() {
        ClassificationResult result = classificationService.classifyTicket(
                "Cannot access account",
                "I cannot login to my account with my password");

        assertNotNull(result.getReasoning());
        assertFalse(result.getReasoning().isEmpty());
        assertTrue(result.getReasoning().contains("Classified as"));
    }

    @Test
    @DisplayName("Classification - Case insensitive keyword matching")
    void testCaseInsensitiveMatching() {
        ClassificationResult result1 = classificationService.classifyTicket(
                "LOGIN issue",
                "Cannot LOGIN to account");

        ClassificationResult result2 = classificationService.classifyTicket(
                "login issue",
                "cannot login to account");

        assertEquals(result1.getCategory(), result2.getCategory());
        assertEquals(result1.getPriority(), result2.getPriority());
    }

    @Test
    @DisplayName("Reclassify ticket - Should update classification")
    void testReclassifyTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setSubject("Database backup failed");
        ticket.setDescription("The database backup failed with error 500 exception");

        classificationService.reclassifyTicket(ticket);

        assertNotNull(ticket.getClassification());
        assertEquals(Category.TECHNICAL_ISSUE, ticket.getCategory());
        assertTrue(ticket.getPriority() != null);
    }

    @Test
    @DisplayName("Manual override - Should mark classification as manual override")
    void testManualOverride() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());

        ClassificationResult override = ClassificationResult.builder()
                .category(Category.BILLING_QUESTION)
                .priority(Priority.HIGH)
                .build();

        ClassificationResult result = classificationService.manualOverride(ticket, override);

        assertTrue(result.getIsManualOverride());
        assertNotNull(result.getClassifiedAt());
    }
}
