package com.support.service.impl;

import com.support.domain.Ticket;
import com.support.dto.BulkImportRequest;
import com.support.dto.BulkImportResponse;
import com.support.dto.CreateTicketRequest;
import com.support.dto.TicketResponse;
import com.support.exception.ImportException;
import com.support.exception.ValidationException;
import com.support.service.ImportService;
import com.support.service.TicketService;
import com.support.service.ValidationService;
import com.support.util.parser.FileParser;
import com.support.util.parser.FileParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportServiceImpl implements ImportService {

    private final TicketService ticketService;
    private final ValidationService validationService;
    private final FileParserFactory fileParserFactory;

    @Value("${app.import.batch-size:100}")
    private int batchSize;

    @Value("${app.import.max-file-size:104857600}")
    private long maxFileSize;

    @Override
    @Transactional
    public BulkImportResponse importTickets(BulkImportRequest request) {
        long startTime = System.currentTimeMillis();
        MultipartFile file = request.getFile();

        // Validate file
        validateFile(file);

        String format = determineFormat(request.getFormat(), file.getOriginalFilename());
        String importBatch = request.getImportBatch() != null ?
                request.getImportBatch() : UUID.randomUUID().toString();

        log.info("Starting import - Format: {}, Batch: {}, File: {}",
                format, importBatch, file.getOriginalFilename());

        List<CreateTicketRequest> parsedTickets;
        try (InputStream inputStream = file.getInputStream()) {
            FileParser parser = fileParserFactory.getParser(format);
            parsedTickets = parser.parse(inputStream);
        } catch (Exception e) {
            log.error("Failed to parse file: {}", e.getMessage());
            throw new ImportException("Failed to parse file: " + e.getMessage(), e);
        }

        if (parsedTickets.isEmpty()) {
            throw new ImportException("No valid tickets found in the file");
        }

        // If validate only, return validation results
        if (Boolean.TRUE.equals(request.getValidateOnly())) {
            return validateTickets(parsedTickets, importBatch, startTime, format);
        }

        // Process tickets in batches
        return processBatches(parsedTickets, importBatch, startTime, format);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new ValidationException(
                    String.format("File size exceeds maximum allowed size of %d MB",
                            maxFileSize / (1024 * 1024)));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new ValidationException("File must have a name");
        }
    }

    private String determineFormat(String providedFormat, String filename) {
        if (providedFormat != null && !providedFormat.isEmpty()) {
            return providedFormat.toUpperCase();
        }

        // Auto-detect from filename
        try {
            FileParser parser = fileParserFactory.detectParser(filename);
            return parser.getSupportedFormat();
        } catch (Exception e) {
            throw new ValidationException(
                    "Cannot determine file format. Please specify format parameter (CSV, JSON, or XML)");
        }
    }

    private BulkImportResponse validateTickets(List<CreateTicketRequest> tickets,
                                              String importBatch,
                                              long startTime,
                                              String format) {
        List<BulkImportResponse.ImportedTicket> validTickets = new ArrayList<>();
        List<BulkImportResponse.FailedRecord> failedRecords = new ArrayList<>();

        int rowNumber = 1;
        for (CreateTicketRequest ticket : tickets) {
            try {
                validationService.validateTicketData(ticket);
                validTickets.add(BulkImportResponse.ImportedTicket.builder()
                        .rowNumber(rowNumber)
                        .subject(ticket.getSubject())
                        .customerId(ticket.getCustomerId())
                        .build());
            } catch (ValidationException e) {
                failedRecords.add(BulkImportResponse.FailedRecord.builder()
                        .rowNumber(rowNumber)
                        .reason(e.getMessage())
                        .data(ticket.getSubject())
                        .build());
            }
            rowNumber++;
        }

        long processingTime = System.currentTimeMillis() - startTime;

        return BulkImportResponse.builder()
                .importBatch(importBatch)
                .importedAt(LocalDateTime.now())
                .totalRecords(tickets.size())
                .successCount(validTickets.size())
                .failureCount(failedRecords.size())
                .processingTimeMs(processingTime)
                .format(format)
                .successfulTickets(validTickets)
                .failedRecords(failedRecords)
                .build();
    }

    private BulkImportResponse processBatches(List<CreateTicketRequest> tickets,
                                             String importBatch,
                                             long startTime,
                                             String format) {
        List<BulkImportResponse.ImportedTicket> successfulTickets = new ArrayList<>();
        List<BulkImportResponse.FailedRecord> failedRecords = new ArrayList<>();

        // Process in batches for better performance
        int totalBatches = (tickets.size() + batchSize - 1) / batchSize;
        log.info("Processing {} tickets in {} batches of size {}",
                tickets.size(), totalBatches, batchSize);

        for (int i = 0; i < tickets.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tickets.size());
            List<CreateTicketRequest> batch = tickets.subList(i, end);
            int batchNumber = (i / batchSize) + 1;

            log.debug("Processing batch {}/{} with {} tickets",
                    batchNumber, totalBatches, batch.size());

            processBatch(batch, importBatch, i + 1, successfulTickets, failedRecords);
        }

        long processingTime = System.currentTimeMillis() - startTime;

        BulkImportResponse response = BulkImportResponse.builder()
                .importBatch(importBatch)
                .importedAt(LocalDateTime.now())
                .totalRecords(tickets.size())
                .successCount(successfulTickets.size())
                .failureCount(failedRecords.size())
                .processingTimeMs(processingTime)
                .format(format)
                .successfulTickets(successfulTickets)
                .failedRecords(failedRecords)
                .build();

        log.info("Import completed - Total: {}, Success: {}, Failed: {}, Time: {}ms",
                response.getTotalRecords(), response.getSuccessCount(),
                response.getFailureCount(), response.getProcessingTimeMs());

        return response;
    }

    @Transactional(noRollbackFor = ValidationException.class)
    private void processBatch(List<CreateTicketRequest> batch,
                             String importBatch,
                             int startRowNumber,
                             List<BulkImportResponse.ImportedTicket> successfulTickets,
                             List<BulkImportResponse.FailedRecord> failedRecords) {
        int rowNumber = startRowNumber;

        for (CreateTicketRequest ticketRequest : batch) {
            try {
                // Add import batch to metadata
                if (ticketRequest.getSource() == null) {
                    ticketRequest.setSource("import");
                }

                // Create ticket
                TicketResponse createdTicket = ticketService.createTicket(ticketRequest);

                successfulTickets.add(BulkImportResponse.ImportedTicket.builder()
                        .ticketId(createdTicket.getId())
                        .rowNumber(rowNumber)
                        .subject(createdTicket.getSubject())
                        .customerId(createdTicket.getCustomerId())
                        .build());

            } catch (Exception e) {
                log.warn("Failed to create ticket at row {}: {}", rowNumber, e.getMessage());

                failedRecords.add(BulkImportResponse.FailedRecord.builder()
                        .rowNumber(rowNumber)
                        .reason(e.getMessage())
                        .data(String.format("Subject: %s, Customer: %s",
                                ticketRequest.getSubject(),
                                ticketRequest.getCustomerId()))
                        .build());
            }
            rowNumber++;
        }
    }
}