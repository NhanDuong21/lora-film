package com.lorafilm.booking.payment.adapter;

import com.lorafilm.booking.payment.dto.PaymentRequestDto;
import com.lorafilm.booking.payment.dto.PaymentResponseDto;
import com.lorafilm.booking.payment.port.PaymentIntegrationPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile({"local", "test"})
public class MockPaymentAdapter implements PaymentIntegrationPort {

    @Override
    public PaymentResponseDto requestPayment(PaymentRequestDto request) {
        String txnCode = "MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // Redirect url will point to the mock payment endpoint
        String mockPaymentUrl = "http://localhost:8083/api/mock/payment/page?bookingCode=" + request.bookingCode();

        return new PaymentResponseDto(
            System.currentTimeMillis(), // Mock payment ID
            request.bookingId(),
            txnCode,
            "PENDING",
            request.amount(),
            request.currency(),
            mockPaymentUrl,
            "MOCK-GATEWAY-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
}
