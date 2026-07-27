package com.project.paymentservice.provider;

import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.entity.Payment;

import java.util.Map;
import java.util.Optional;

public interface PaymentProvider {
    ProviderCode providerCode();
    PaymentSession createSession(PaymentSessionRequest request);
    ProviderCallbackResult verifyCallback(Map<String, String> parameters, String rawBody);
    ProviderCallbackResult verifyReturn(Map<String, String> parameters);

    default Optional<ProviderCallbackResult> queryStatus(Payment payment) {
        return Optional.empty();
    }
}
