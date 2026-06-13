package com.project.authservice.service;

import com.project.authservice.entity.Account;

public interface VerificationService {
    void generateVerification(Account account);
    void verify(Long accountId, String code);
}
