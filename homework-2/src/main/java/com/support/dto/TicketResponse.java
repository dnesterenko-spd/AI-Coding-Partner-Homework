package com.support.dto;

import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private UUID id;
    private String customerId;
    private String customerEmail;
    private String customerName;
    private String subject;
    private String description;
    private Category category;
    private Priority priority;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String assignedTo;
    private Set<String> tags;
    private TicketMetadataDto metadata;
    private ClassificationResultDto classification;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketMetadataDto {
        private String source;
        private String browser;
        private String deviceType;
        private String ipAddress;
        private String importBatch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationResultDto {
        private Category category;
        private Priority priority;
        private Double confidenceScore;
        private List<String> matchedKeywords;
        private String reasoning;
        private LocalDateTime classifiedAt;
        private Boolean isManualOverride;
    }
}