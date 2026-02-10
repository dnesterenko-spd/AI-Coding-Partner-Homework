package com.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResponse {

    private String importBatch;
    private LocalDateTime importedAt;
    private Integer totalRecords;
    private Integer successCount;
    private Integer failureCount;
    private Long processingTimeMs;
    private String format;

    @Builder.Default
    private List<ImportedTicket> successfulTickets = new ArrayList<>();

    @Builder.Default
    private List<FailedRecord> failedRecords = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportedTicket {
        private UUID ticketId;
        private String subject;
        private String customerId;
        private Integer rowNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedRecord {
        private Integer rowNumber;
        private String reason;
        private String data;
    }
}