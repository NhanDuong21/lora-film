package com.lorafilm.movie.common.exception;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object errorData;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorData = null;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorData = null;
    }

    public BusinessException(ErrorCode errorCode, Object errorData) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorData = errorData;
    }

    public BusinessException(ErrorCode errorCode, String message, Object errorData) {
        super(message);
        this.errorCode = errorCode;
        this.errorData = errorData;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getErrorData() {
        return errorData;
    }
}

