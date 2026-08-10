package com.project.paymentservice.provider.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentRefund;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.provider.ProviderRefundResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "payment.providers.mock.enabled", havingValue = "true")
public class MockPaymentProvider implements PaymentProvider {
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public MockPaymentProvider(
            ObjectMapper objectMapper,
            @Value("${payment.providers.mock.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public ProviderCode providerCode() {
        return ProviderCode.MOCK;
    }

    @Override
    public PaymentSession createSession(PaymentSessionRequest request) {
        String orderId = "MOCK-" + request.getPaymentTransactionCode();
        PaymentSession session = new PaymentSession(
                orderId,
                orderId,
                baseUrl + "/payments/mock/" + request.getPaymentPublicId(),
                min(request.getExpiresAt(), Instant.now().plusSeconds(900)));
        session.setSanitizedProviderSummary("{\"provider\":\"MOCK\"}");
        return session;
    }

    @Override
    public ProviderCallbackResult verifyCallback(Map<String, String> parameters, String rawBody) {
        throw new UnsupportedOperationException("MOCK callbacks use the authenticated test endpoint");
    }

    @Override
    public ProviderCallbackResult verifyReturn(Map<String, String> parameters) {
        throw new UnsupportedOperationException("MOCK does not use provider return");
    }

    @Override
    public ProviderRefundResult refund(Payment payment, PaymentRefund refund) {
        ProviderRefundResult result = new ProviderRefundResult();
        result.setState(ProviderRefundResult.State.SUCCESS);
        result.setProviderOrderId(refund.getRefundCode());
        result.setProviderRequestId(refund.getPublicId());
        result.setProviderRefundId("MOCK-REFUND-" + refund.getPublicId());
        result.setResponseCode("00");
        result.setSummarySanitized("{\"provider\":\"MOCK\",\"result\":\"SUCCESS\"}");
        return result;
    }

    private Instant min(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }
}
