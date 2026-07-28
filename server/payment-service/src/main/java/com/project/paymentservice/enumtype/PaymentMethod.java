package com.project.paymentservice.enumtype;

public enum PaymentMethod {
    ONLINE,
    CASH,
    /**
     * Compatibility-only values for old API/tests. New production records are
     * normalized to ONLINE plus a separate ProviderCode.
     */
    @Deprecated
    MOCK,
    @Deprecated
    VNPAY,
    @Deprecated
    MOMO
}
