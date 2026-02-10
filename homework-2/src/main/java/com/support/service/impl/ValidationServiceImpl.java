package com.support.service.impl;

import com.support.domain.Category;
import com.support.domain.Priority;
import com.support.domain.Status;
import com.support.dto.CreateTicketRequest;
import com.support.exception.ValidationException;
import com.support.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ValidationServiceImpl implements ValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    public void validateTicketData(CreateTicketRequest request) {
        Map<String, String> errors = new HashMap<>();

        // Validate customer ID
        if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
            errors.put("customerId", "Customer ID is required");
        }

        // Validate customer email
        if (request.getCustomerEmail() == null || request.getCustomerEmail().trim().isEmpty()) {
            errors.put("customerEmail", "Customer email is required");
        } else if (!isValidEmail(request.getCustomerEmail())) {
            errors.put("customerEmail", "Invalid email format");
        }

        // Validate customer name
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            errors.put("customerName", "Customer name is required");
        }

        // Validate subject
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            errors.put("subject", "Subject is required");
        } else if (request.getSubject().length() > 200) {
            errors.put("subject", "Subject must not exceed 200 characters");
        }

        // Validate description
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            errors.put("description", "Description is required");
        } else if (request.getDescription().length() < 10) {
            errors.put("description", "Description must be at least 10 characters");
        } else if (request.getDescription().length() > 2000) {
            errors.put("description", "Description must not exceed 2000 characters");
        }

        // Validate enums if provided
        if (request.getCategory() != null && !isValidEnumValue(request.getCategory().name(), Category.class)) {
            errors.put("category", "Invalid category value");
        }

        if (request.getPriority() != null && !isValidEnumValue(request.getPriority().name(), Priority.class)) {
            errors.put("priority", "Invalid priority value");
        }

        if (request.getStatus() != null && !isValidEnumValue(request.getStatus().name(), Status.class)) {
            errors.put("status", "Invalid status value");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }
    }

    @Override
    public void validateImportRecord(Map<String, String> record) {
        Map<String, String> errors = new HashMap<>();

        // Required fields
        String[] requiredFields = {"customer_id", "customer_email", "customer_name", "subject", "description"};

        for (String field : requiredFields) {
            if (!record.containsKey(field) || record.get(field) == null || record.get(field).trim().isEmpty()) {
                errors.put(field, field + " is required");
            }
        }

        // Validate email format
        if (record.containsKey("customer_email") && record.get("customer_email") != null) {
            String email = record.get("customer_email").trim();
            if (!email.isEmpty() && !isValidEmail(email)) {
                errors.put("customer_email", "Invalid email format");
            }
        }

        // Validate subject length
        if (record.containsKey("subject") && record.get("subject") != null) {
            String subject = record.get("subject");
            if (subject.length() > 200) {
                errors.put("subject", "Subject must not exceed 200 characters");
            }
        }

        // Validate description length
        if (record.containsKey("description") && record.get("description") != null) {
            String description = record.get("description");
            if (description.length() < 10) {
                errors.put("description", "Description must be at least 10 characters");
            } else if (description.length() > 2000) {
                errors.put("description", "Description must not exceed 2000 characters");
            }
        }

        // Validate enum values if present
        if (record.containsKey("category") && record.get("category") != null && !record.get("category").isEmpty()) {
            if (!isValidEnumValue(record.get("category").toUpperCase().replace(" ", "_"), Category.class)) {
                errors.put("category", "Invalid category value: " + record.get("category"));
            }
        }

        if (record.containsKey("priority") && record.get("priority") != null && !record.get("priority").isEmpty()) {
            if (!isValidEnumValue(record.get("priority").toUpperCase(), Priority.class)) {
                errors.put("priority", "Invalid priority value: " + record.get("priority"));
            }
        }

        if (record.containsKey("status") && record.get("status") != null && !record.get("status").isEmpty()) {
            if (!isValidEnumValue(record.get("status").toUpperCase().replace(" ", "_"), Status.class)) {
                errors.put("status", "Invalid status value: " + record.get("status"));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Record validation failed", errors);
        }
    }

    @Override
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    @Override
    public boolean isValidEnumValue(String value, Class<? extends Enum<?>> enumClass) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumType = (Class<? extends Enum>) enumClass;
            Enum.valueOf(enumType, value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Map<String, String> validateImportBatch(Map<String, String>[] records) {
        Map<String, String> batchErrors = new HashMap<>();

        for (int i = 0; i < records.length; i++) {
            try {
                validateImportRecord(records[i]);
            } catch (ValidationException e) {
                batchErrors.put("Row " + (i + 1), e.getMessage());
            }
        }

        return batchErrors;
    }
}