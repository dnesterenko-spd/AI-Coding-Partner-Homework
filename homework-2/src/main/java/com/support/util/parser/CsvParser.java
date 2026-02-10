package com.support.util.parser;

import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import com.support.dto.CreateTicketRequest;
import com.support.exception.ImportException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class CsvParser implements FileParser {

    private static final String[] REQUIRED_HEADERS = {
            "customer_id", "customer_email", "customer_name", "subject", "description"
    };

    @Override
    public List<CreateTicketRequest> parse(InputStream inputStream) throws Exception {
        List<CreateTicketRequest> tickets = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            // Validate headers
            Set<String> headers = csvParser.getHeaderMap().keySet();
            validateHeaders(headers);

            int recordNumber = 1;
            for (CSVRecord record : csvParser) {
                try {
                    CreateTicketRequest ticket = parseRecord(record);
                    tickets.add(ticket);
                } catch (Exception e) {
                    String errorMsg = String.format("Row %d: %s", recordNumber + 1, e.getMessage());
                    errors.add(errorMsg);
                    log.warn("Failed to parse CSV record at row {}: {}", recordNumber + 1, e.getMessage());
                }
                recordNumber++;
            }

            if (!errors.isEmpty() && tickets.isEmpty()) {
                throw new ImportException("All records failed validation", errors,
                        recordNumber, 0);
            }

            log.info("Successfully parsed {} tickets from CSV", tickets.size());
            return tickets;

        } catch (Exception e) {
            log.error("Error parsing CSV file: {}", e.getMessage());
            throw new ImportException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    private void validateHeaders(Set<String> headers) {
        Set<String> missingHeaders = new HashSet<>();
        for (String required : REQUIRED_HEADERS) {
            boolean found = headers.stream()
                    .anyMatch(h -> h.toLowerCase().replace(" ", "_").equals(required));
            if (!found) {
                missingHeaders.add(required);
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw new ImportException("Missing required headers: " + String.join(", ", missingHeaders));
        }
    }

    private CreateTicketRequest parseRecord(CSVRecord record) {
        CreateTicketRequest.CreateTicketRequestBuilder builder = CreateTicketRequest.builder();

        // Required fields
        builder.customerId(getRequiredField(record, "customer_id"));
        builder.customerEmail(getRequiredField(record, "customer_email"));
        builder.customerName(getRequiredField(record, "customer_name"));
        builder.subject(getRequiredField(record, "subject"));
        builder.description(getRequiredField(record, "description"));

        // Optional fields
        String category = getOptionalField(record, "category");
        if (category != null && !category.isEmpty()) {
            try {
                builder.category(Category.valueOf(category.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid category value: {}, using default", category);
            }
        }

        String priority = getOptionalField(record, "priority");
        if (priority != null && !priority.isEmpty()) {
            try {
                builder.priority(Priority.valueOf(priority.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid priority value: {}, using default", priority);
            }
        }

        String status = getOptionalField(record, "status");
        if (status != null && !status.isEmpty()) {
            try {
                builder.status(Status.valueOf(status.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid status value: {}, using default", status);
            }
        }

        builder.assignedTo(getOptionalField(record, "assigned_to"));
        builder.source(getOptionalField(record, "source"));
        builder.browser(getOptionalField(record, "browser"));
        builder.deviceType(getOptionalField(record, "device_type"));

        // Parse tags if present
        String tagsStr = getOptionalField(record, "tags");
        if (tagsStr != null && !tagsStr.isEmpty()) {
            Set<String> tags = new HashSet<>();
            for (String tag : tagsStr.split(",")) {
                tags.add(tag.trim());
            }
            builder.tags(tags);
        }

        return builder.build();
    }

    private String getRequiredField(CSVRecord record, String fieldName) {
        String value = getOptionalField(record, fieldName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        return value;
    }

    private String getOptionalField(CSVRecord record, String fieldName) {
        // Try different variations of the field name
        String[] variations = {
                fieldName,
                fieldName.replace("_", " "),
                fieldName.replace("_", ""),
                fieldName.replace("_", "-")
        };

        for (String variation : variations) {
            if (record.isMapped(variation)) {
                String value = record.get(variation);
                return value != null ? value.trim() : null;
            }
            // Try case-insensitive match
            String lowerVariation = variation.toLowerCase();
            if (record.isMapped(lowerVariation)) {
                String value = record.get(lowerVariation);
                return value != null ? value.trim() : null;
            }
        }

        return null;
    }

    @Override
    public String getSupportedFormat() {
        return "CSV";
    }
}