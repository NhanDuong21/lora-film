package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class AccountInactiveException extends BaseAuthException {
    public AccountInactiveException() {
        super("Account is not active", "AUTH_ACCOUNT_INACTIVE", HttpStatus.FORBIDDEN);
    }
}
