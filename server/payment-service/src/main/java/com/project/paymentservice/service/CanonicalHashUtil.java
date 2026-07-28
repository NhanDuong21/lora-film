package com.project.paymentservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class CanonicalHashUtil {

    private CanonicalHashUtil() {
    }

    public static String hashCreatePayment(Long accountId, String bookingReference, String paymentMethod) {
        String canonical = "operation=CREATE_PAYMENT"
                + "|accountId=" + accountId
                + "|bookingReference=" + bookingReference
                + "|paymentMethod=" + paymentMethod;
        return sha256Hex(canonical);
    }

    public static String hashCreatePayment(Long accountId, Long bookingId, String paymentMethod) {
        return hashCreatePayment(accountId, String.valueOf(bookingId), paymentMethod);
    }

    public static String hashCancelPayment(Long accountId, Long paymentId) {
        String canonical = "operation=CANCEL_PAYMENT"
                + "|accountId=" + accountId
                + "|paymentId=" + paymentId;
        return sha256Hex(canonical);
    }

    public static String hashOperation(String operation, Long accountId, String canonicalPayload) {
        return sha256Hex("operation=" + operation
                + "|accountId=" + accountId
                + "|payload=" + (canonicalPayload == null ? "" : canonicalPayload.trim()));
    }

    public static String hashCollectCashPayment(Long accountId, Long paymentId, java.math.BigDecimal receivedAmount, String note) {
        String canonical = "operation=COLLECT_CASH_PAYMENT"
                + "|accountId=" + accountId
                + "|paymentId=" + paymentId
                + "|receivedAmount=" + receivedAmount
                + "|note=" + (note == null ? "" : note.trim());
        return sha256Hex(canonical);
    }

    public static String hashCancelCashPayment(Long accountId, Long paymentId, String reason) {
        String canonical = "operation=CANCEL_CASH_PAYMENT"
                + "|accountId=" + accountId
                + "|paymentId=" + paymentId
                + "|reason=" + (reason == null ? "" : reason.trim());
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
