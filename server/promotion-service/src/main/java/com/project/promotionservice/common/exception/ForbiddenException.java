package com.project.promotionservice.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }
}
