package com.project.paymentservice.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TransactionCodeGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SUFFIX_LENGTH = 8;

    public String generate(Long bookingId) {
        StringBuilder sb = new StringBuilder("PAY-");
        sb.append(bookingId).append("-");
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
