package com.project.paymentservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class CanonicalHashUtil {

    private CanonicalHashUtil() {
    }

    public static String hashCreatePayment(Long accountId, Long bookingId, String paymentMethod) {
        String canonical = "operation=CREATE_PAYMENT"
                + "|accountId=" + accountId
                + "|bookingId=" + bookingId
                + "|paymentMethod=" + paymentMethod;
        return sha256Hex(canonical);
    }

    public static String hashCancelPayment(Long accountId, Long paymentId) {
        String canonical = "operation=CANCEL_PAYMENT"
                + "|accountId=" + accountId
                + "|paymentId=" + paymentId;
        return sha256Hex(canonical);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
