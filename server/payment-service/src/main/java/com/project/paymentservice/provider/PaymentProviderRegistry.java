package com.project.paymentservice.provider;

import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProviderRegistry {

    private final Map<PaymentMethod, PaymentProvider> providers;

    public PaymentProviderRegistry(List<PaymentProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(PaymentProvider::supportedMethod, Function.identity()));
    }

    public PaymentProvider getProvider(PaymentMethod method) {
        PaymentProvider provider = providers.get(method);
        if (provider == null) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Unsupported payment method: " + method, HttpStatus.BAD_REQUEST);
        }
        return provider;
    }

    public boolean isSupported(PaymentMethod method) {
        return providers.containsKey(method);
    }
}
