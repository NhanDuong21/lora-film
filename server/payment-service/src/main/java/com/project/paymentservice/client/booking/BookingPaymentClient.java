package com.project.paymentservice.client.booking;

public interface BookingPaymentClient {
    BookingPaymentContext getPaymentContext(Long bookingId);
}
