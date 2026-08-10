package com.project.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.repository.PaymentWebhookEventRepository;
import com.project.paymentservice.service.PaymentTransactionService;
import com.project.paymentservice.service.ProviderCallbackService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderReturnFallbackTest {

    @Test
    void verifiedReturnImmediatelyUsesAuthoritativeStatusQuery() {
        Fixture fixture = fixture(true);
        when(fixture.provider.queryStatus(fixture.payment))
                .thenReturn(Optional.of(fixture.result));

        ProviderCallbackService.ReturnOutcome outcome = fixture.service.verifyReturn(
                ProviderCode.VNPAY, Map.of("vnp_TxnRef", "PAY001"));

        assertEquals("payment-public-id", outcome.paymentPublicId());
        verify(fixture.transactionService).applyProviderResult(
                ProviderCode.VNPAY, fixture.result, null);
        verify(fixture.transactionService, never())
                .scheduleProviderStatusCheck(any(), any());
    }

    @Test
    void verifiedReturnSchedulesRateLimitedRetryWhenProviderIsNotTerminal() {
        Fixture fixture = fixture(true);
        when(fixture.provider.queryStatus(fixture.payment)).thenReturn(Optional.empty());
        when(fixture.provider.recoveryRetryDelaySeconds()).thenReturn(305);

        fixture.service.verifyReturn(
                ProviderCode.VNPAY, Map.of("vnp_TxnRef", "PAY001"));

        verify(fixture.transactionService).scheduleProviderStatusCheck(
                eq(13L), any());
        verify(fixture.transactionService, never()).applyProviderResult(
                any(), any(), any());
    }

    @Test
    void invalidReturnDoesNotScheduleStatusQueryOrMutatePayment() {
        Fixture fixture = fixture(false);

        ProviderCallbackService.ReturnOutcome outcome = fixture.service.verifyReturn(
                ProviderCode.VNPAY, Map.of("vnp_TxnRef", "PAY001"));

        assertEquals(false, outcome.signatureValid());
        verify(fixture.transactionService, never())
                .scheduleProviderStatusCheck(any(), any());
        verify(fixture.transactionService, never()).applyProviderResult(
                any(), any(), any());
    }

    private Fixture fixture(boolean signatureValid) {
        PaymentProviderRegistry registry = mock(PaymentProviderRegistry.class);
        PaymentProvider provider = mock(PaymentProvider.class);
        PaymentWebhookEventRepository webhookRepository =
                mock(PaymentWebhookEventRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentTransactionService transactionService =
                mock(PaymentTransactionService.class);

        ProviderCallbackResult result = new ProviderCallbackResult();
        result.setSignatureValid(signatureValid);
        result.setProviderOrderId("PAY001");
        result.setExternalTransactionId("123456");
        result.setAmount(new BigDecimal("355000"));
        result.setCurrency("VND");
        result.setResult("SUCCESS");

        Payment payment = new Payment();
        payment.setId(13L);
        payment.setPublicId("payment-public-id");
        payment.setBookingPublicId("booking-public-id");
        payment.setStatus(PaymentStatus.PROCESSING);

        when(registry.getProvider(ProviderCode.VNPAY)).thenReturn(provider);
        when(provider.verifyReturn(any())).thenReturn(result);
        if (signatureValid) {
            when(paymentRepository.findByProviderCodeAndProviderOrderId(
                    ProviderCode.VNPAY, "PAY001")).thenReturn(Optional.of(payment));
        }

        ProviderCallbackService service = new ProviderCallbackService(
                registry,
                webhookRepository,
                paymentRepository,
                transactionService,
                new ObjectMapper());
        return new Fixture(service, transactionService, provider, payment, result);
    }

    private record Fixture(
            ProviderCallbackService service,
            PaymentTransactionService transactionService,
            PaymentProvider provider,
            Payment payment,
            ProviderCallbackResult result) {
    }
}
