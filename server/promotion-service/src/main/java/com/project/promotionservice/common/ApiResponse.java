package com.project.promotionservice.common;

import java.util.List;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private String errorCode;
    private T data;
    private List<ValidationError> errors;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, String errorCode, T data, List<ValidationError> errors) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
        this.data = data;
        this.errors = errors;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
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

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, null, data, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, errorCode, null, null);
    }

    public static <T> ApiResponse<T> validationError(String message, List<ValidationError> errors) {
        return new ApiResponse<>(false, message, "VALIDATION_ERROR", null, errors);
    }
}
