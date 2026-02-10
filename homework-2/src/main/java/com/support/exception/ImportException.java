package com.support.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ImportException extends RuntimeException {

    private List<String> failedRecords;
    private int totalRecords;
    private int successCount;

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, List<String> failedRecords, int totalRecords, int successCount) {
        super(message);
        this.failedRecords = failedRecords;
        this.totalRecords = totalRecords;
        this.successCount = successCount;
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public List<String> getFailedRecords() {
        return failedRecords;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return totalRecords - successCount;
    }
}