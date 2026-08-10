package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends BaseAuthException {
    public AccountNotFoundException() {
        super("Account not found", "AUTH_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
