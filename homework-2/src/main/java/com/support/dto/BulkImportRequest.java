package com.support.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportRequest {

    @NotNull(message = "File is required")
    private MultipartFile file;

    private String format; // CSV, JSON, or XML - will be auto-detected if not provided
    private Boolean validateOnly; // If true, only validate without importing
    private String importBatch; // Optional batch identifier
}