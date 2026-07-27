package com.lorafilm.booking.common.response;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {

    private boolean success = false;
    private String errorCode;
    private String message;
    private List<ValidationErrorDetail> details;
    private String reconciliationTaskPublicId;
    private Instant timestamp;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String errorCode, String message, List<ValidationErrorDetail> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ValidationErrorDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ValidationErrorDetail> details) {
        this.details = details;
    }

    public String getReconciliationTaskPublicId() {
        return reconciliationTaskPublicId;
    }

    public void setReconciliationTaskPublicId(String reconciliationTaskPublicId) {
        this.reconciliationTaskPublicId = reconciliationTaskPublicId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public static class ValidationErrorDetail {
        private String field;
        private String message;

        public ValidationErrorDetail() {
        }

        public ValidationErrorDetail(String field, String message) {
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
