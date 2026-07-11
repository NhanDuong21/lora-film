package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class AccountAlreadyVerifiedException extends BaseAuthException {
    public AccountAlreadyVerifiedException() {
        super("Account is already verified", "AUTH_ACCOUNT_ALREADY_VERIFIED", HttpStatus.CONFLICT);
    }
}
