package com.support.util.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import com.support.dto.CreateTicketRequest;
import com.support.exception.ImportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Component
@Slf4j
public class JsonParser implements FileParser {

    private final ObjectMapper objectMapper;

    public JsonParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    @Override
    public List<CreateTicketRequest> parse(InputStream inputStream) throws Exception {
        List<CreateTicketRequest> tickets = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(inputStream);

            // Handle both array and object with tickets property
            JsonNode ticketsNode;
            if (rootNode.isArray()) {
                ticketsNode = rootNode;
            } else if (rootNode.has("tickets") && rootNode.get("tickets").isArray()) {
                ticketsNode = rootNode.get("tickets");
            } else {
                throw new ImportException("Invalid JSON format. Expected array or object with 'tickets' array");
            }

            int recordNumber = 0;
            for (JsonNode node : ticketsNode) {
                recordNumber++;
                try {
                    CreateTicketRequest ticket = parseJsonNode(node);
                    tickets.add(ticket);
                } catch (Exception e) {
                    String errorMsg = String.format("Record %d: %s", recordNumber, e.getMessage());
                    errors.add(errorMsg);
                    log.warn("Failed to parse JSON record at position {}: {}", recordNumber, e.getMessage());
                }
            }

            if (!errors.isEmpty() && tickets.isEmpty()) {
                throw new ImportException("All records failed validation", errors,
                        recordNumber, 0);
            }

            log.info("Successfully parsed {} tickets from JSON", tickets.size());
            return tickets;

        } catch (ImportException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing JSON file: {}", e.getMessage());
            throw new ImportException("Failed to parse JSON file: " + e.getMessage(), e);
        }
    }

    private CreateTicketRequest parseJsonNode(JsonNode node) {
        CreateTicketRequest.CreateTicketRequestBuilder builder = CreateTicketRequest.builder();

        // Required fields
        builder.customerId(getRequiredTextField(node, "customer_id", "customerId"));
        builder.customerEmail(getRequiredTextField(node, "customer_email", "customerEmail"));
        builder.customerName(getRequiredTextField(node, "customer_name", "customerName"));
        builder.subject(getRequiredTextField(node, "subject"));
        builder.description(getRequiredTextField(node, "description"));

        // Optional fields
        String category = getOptionalTextField(node, "category");
        if (category != null && !category.isEmpty()) {
            try {
                builder.category(Category.valueOf(category.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid category value: {}, using default", category);
            }
        }

        String priority = getOptionalTextField(node, "priority");
        if (priority != null && !priority.isEmpty()) {
            try {
                builder.priority(Priority.valueOf(priority.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid priority value: {}, using default", priority);
            }
        }

        String status = getOptionalTextField(node, "status");
        if (status != null && !status.isEmpty()) {
            try {
                builder.status(Status.valueOf(status.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) {
                log.debug("Invalid status value: {}, using default", status);
            }
        }

        builder.assignedTo(getOptionalTextField(node, "assigned_to", "assignedTo"));
        builder.source(getOptionalTextField(node, "source"));
        builder.browser(getOptionalTextField(node, "browser"));
        builder.deviceType(getOptionalTextField(node, "device_type", "deviceType"));

        // Parse tags if present
        JsonNode tagsNode = node.get("tags");
        if (tagsNode != null) {
            Set<String> tags = new HashSet<>();
            if (tagsNode.isArray()) {
                for (JsonNode tagNode : tagsNode) {
                    tags.add(tagNode.asText());
                }
            } else if (tagsNode.isTextual()) {
                // Handle comma-separated tags
                for (String tag : tagsNode.asText().split(",")) {
                    tags.add(tag.trim());
                }
            }
            builder.tags(tags);
        }

        return builder.build();
    }

    private String getRequiredTextField(JsonNode node, String... fieldNames) {
        String value = getOptionalTextField(node, fieldNames);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required field: " + fieldNames[0]);
        }
        return value;
    }

    private String getOptionalTextField(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                return fieldNode.asText().trim();
            }
        }
        return null;
    }

    @Override
    public String getSupportedFormat() {
        return "JSON";
    }
}