package com.project.paymentservice.provider;

import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentRefund;

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

    default int recoveryRetryDelaySeconds() {
        return 0;
    }

    default ProviderRefundResult refund(Payment payment, PaymentRefund refund) {
        ProviderRefundResult result = new ProviderRefundResult();
        result.setState(ProviderRefundResult.State.FAILED);
        result.setFailureCode("REFUND_PROVIDER_UNSUPPORTED");
        result.setMessageSanitized("Provider does not support automatic refund");
        return result;
    }

    default Optional<ProviderRefundResult> queryRefund(Payment payment, PaymentRefund refund) {
        return Optional.empty();
    }
}
