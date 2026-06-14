package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class AccountNotVerifiedException extends BaseAuthException {
    public AccountNotVerifiedException() {
        super("Account is not verified", "AUTH_ACCOUNT_NOT_VERIFIED", HttpStatus.FORBIDDEN);
    }
}
