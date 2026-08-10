package com.project.promotionservice.configuration.security.principal;

import java.security.Principal;
import java.util.Locale;
import java.util.Objects;

public final class InternalServicePrincipal implements Principal {

    private final String serviceName;

    public InternalServicePrincipal(String serviceName) {
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    @Override
    public String getName() {
        return serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
