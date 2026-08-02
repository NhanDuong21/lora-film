package com.lorafilm.booking.monitoring.dto;

public class MonitoringSummaryResponse {

    private long bookingToday;
    private long paymentFailed;
    private long expiredBooking;
    private long pendingRetry;
    private long promotionReconciliationMismatch;

    public MonitoringSummaryResponse() {
    }

    public MonitoringSummaryResponse(long bookingToday, long paymentFailed,
                                     long expiredBooking, long pendingRetry,
                                     long promotionReconciliationMismatch) {
        this.bookingToday = bookingToday;
        this.paymentFailed = paymentFailed;
        this.expiredBooking = expiredBooking;
        this.pendingRetry = pendingRetry;
        this.promotionReconciliationMismatch = promotionReconciliationMismatch;
    }

    public long getBookingToday() {
        return bookingToday;
    }

    public void setBookingToday(long bookingToday) {
        this.bookingToday = bookingToday;
    }

    public long getPaymentFailed() {
        return paymentFailed;
    }

    public void setPaymentFailed(long paymentFailed) {
        this.paymentFailed = paymentFailed;
    }

    public long getExpiredBooking() {
        return expiredBooking;
    }

    public void setExpiredBooking(long expiredBooking) {
        this.expiredBooking = expiredBooking;
    }

    public long getPendingRetry() {
        return pendingRetry;
    }

    public void setPendingRetry(long pendingRetry) {
        this.pendingRetry = pendingRetry;
    }

    public long getPromotionReconciliationMismatch() {
        return promotionReconciliationMismatch;
    }

    public void setPromotionReconciliationMismatch(
            long promotionReconciliationMismatch) {
        this.promotionReconciliationMismatch = promotionReconciliationMismatch;
    }
}
