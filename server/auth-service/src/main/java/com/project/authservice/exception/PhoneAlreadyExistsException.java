package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class PhoneAlreadyExistsException extends BaseAuthException {
    public PhoneAlreadyExistsException() {
        super("Phone number already exists", "USER_PHONE_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }
}
