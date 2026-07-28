package com.project.paymentservice.client.booking;

public interface BookingPaymentClient {
    BookingPaymentContext getPaymentContext(Long bookingId);
    BookingPaymentContext getPaymentContext(String bookingPublicId);
    BookingPaymentContext getPaymentContextByCode(String bookingCode);
    BookingPaymentResultResponse notifyPaymentResult(
            String bookingPublicId, BookingPaymentResultRequest request);
}
