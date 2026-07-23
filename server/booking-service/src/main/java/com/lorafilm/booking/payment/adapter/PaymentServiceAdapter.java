package com.lorafilm.booking.payment.adapter;

import com.lorafilm.booking.payment.dto.PaymentRequestDto;
import com.lorafilm.booking.payment.dto.PaymentResponseDto;
import com.lorafilm.booking.payment.port.PaymentIntegrationPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !test")
public class PaymentServiceAdapter implements PaymentIntegrationPort {

    @Override
    public PaymentResponseDto requestPayment(PaymentRequestDto request) {
        throw new UnsupportedOperationException("Production Payment Service Adapter is not implemented yet.");
    }
}
