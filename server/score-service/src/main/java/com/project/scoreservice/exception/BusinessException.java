package com.project.scoreservice.exception;
 
import org.springframework.http.HttpStatus;
 
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;
 
    public BusinessException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
 
    public String getErrorCode() {
        return errorCode;
    }
 
    public final HttpStatus getStatus() {
        return status;
    }
}
