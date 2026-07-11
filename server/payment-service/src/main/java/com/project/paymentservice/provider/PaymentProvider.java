package com.project.paymentservice.provider;

import com.project.paymentservice.enumtype.PaymentMethod;

public interface PaymentProvider {
    PaymentMethod supportedMethod();
    PaymentSession createSession(PaymentSessionRequest request);
}
