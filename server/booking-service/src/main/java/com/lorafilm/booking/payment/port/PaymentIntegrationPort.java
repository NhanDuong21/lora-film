package com.lorafilm.booking.payment.port;

import com.lorafilm.booking.payment.dto.PaymentRequestDto;
import com.lorafilm.booking.payment.dto.PaymentResponseDto;

public interface PaymentIntegrationPort {
    PaymentResponseDto requestPayment(PaymentRequestDto request);
}
