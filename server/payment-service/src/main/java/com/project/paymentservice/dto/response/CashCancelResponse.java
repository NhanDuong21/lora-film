package com.project.paymentservice.dto.response;

import java.time.LocalDateTime;

public class CashCancelResponse {
    private Long paymentId;
    private String status;
    private Long cancelledByAccountId;
    private LocalDateTime cancelledAt;

    public CashCancelResponse(Long paymentId, String status, Long cancelledByAccountId, LocalDateTime cancelledAt) {
        this.paymentId = paymentId;
        this.status = status;
        this.cancelledByAccountId = cancelledByAccountId;
        this.cancelledAt = cancelledAt;
    }

    public Long getPaymentId() { return paymentId; }
    public String getStatus() { return status; }
    public Long getCancelledByAccountId() { return cancelledByAccountId; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
