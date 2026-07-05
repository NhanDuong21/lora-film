package com.project.paymentservice.provider.mock;

import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockPaymentProvider implements PaymentProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentProvider.class);

    @Value("${payment.mock.base-url:http://localhost:8084}")
    private String mockBaseUrl;

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.MOCK;
    }

    @Override
    public PaymentSession createSession(PaymentSessionRequest request) {
        logger.info("Creating MOCK payment session for paymentId={}, amount={}",
                request.getPaymentId(), request.getAmount());

        String providerOrderId = "MOCK-ORDER-" + request.getPaymentId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String providerSessionId = "MOCK-SESSION-" + UUID.randomUUID().toString().substring(0, 12);
        String paymentUrl = mockBaseUrl + "/mock-checkout?orderId=" + providerOrderId
                + "&amount=" + request.getAmount()
                + "&currency=" + request.getCurrency();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        return new PaymentSession(providerOrderId, providerSessionId, paymentUrl, expiresAt);
    }
}
