package com.project.paymentservice.provider;

import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentProviderRegistry {
    private final Map<ProviderCode, PaymentProvider> providers = new EnumMap<>(ProviderCode.class);

    public PaymentProviderRegistry(List<PaymentProvider> providerList) {
        for (PaymentProvider provider : providerList) {
            PaymentProvider previous = providers.put(provider.providerCode(), provider);
            if (previous != null) {
                throw new IllegalStateException("Duplicate provider bean: " + provider.providerCode());
            }
        }
    }

    public PaymentProvider getProvider(ProviderCode code) {
        PaymentProvider provider = providers.get(code);
        if (provider == null) {
            throw new BusinessException("PAYMENT_PROVIDER_DISABLED",
                    "Phương thức thanh toán này chưa được cấu hình",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return provider;
    }

    public boolean isSupported(ProviderCode code) {
        return providers.containsKey(code);
    }
}
