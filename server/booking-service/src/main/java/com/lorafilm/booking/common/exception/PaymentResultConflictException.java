package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A deterministic Payment result rejection whose receipt and optional
 * reconciliation task must remain committed for replay and audit.
 */
public class PaymentResultConflictException extends BusinessException {

    private final String reconciliationTaskPublicId;

    public PaymentResultConflictException(
            String errorCode,
            String message,
            String reconciliationTaskPublicId) {
        super(errorCode, message, HttpStatus.CONFLICT);
        this.reconciliationTaskPublicId = reconciliationTaskPublicId;
    }

    public String getReconciliationTaskPublicId() {
        return reconciliationTaskPublicId;
    }
}
