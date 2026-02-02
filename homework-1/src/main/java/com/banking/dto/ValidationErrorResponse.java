package com.banking.dto;

import java.util.List;

public class ValidationErrorResponse {
    private String error;
    private List<ValidationError> details;

    public ValidationErrorResponse() {
    }

    public ValidationErrorResponse(String error, List<ValidationError> details) {
        this.error = error;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<ValidationError> getDetails() {
        return details;
    }

    public void setDetails(List<ValidationError> details) {
        this.details = details;
    }

    public static class ValidationError {
        private String field;
        private String message;

        public ValidationError() {
        }

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
