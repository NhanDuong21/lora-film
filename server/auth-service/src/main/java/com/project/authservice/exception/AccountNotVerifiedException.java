package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class AccountNotVerifiedException extends BaseAuthException {
    private final Long accountId;

    public AccountNotVerifiedException(Long accountId) {
        super("Account is not verified", "AUTH_ACCOUNT_NOT_VERIFIED", HttpStatus.FORBIDDEN);
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}
