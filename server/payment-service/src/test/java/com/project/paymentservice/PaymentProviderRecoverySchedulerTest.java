package com.project.paymentservice;

import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.service.PaymentProviderRecoveryScheduler;
import com.project.paymentservice.service.PaymentTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProviderRecoverySchedulerTest {

    @Test
    void queriesExpiredProviderSessionsSoLateSuccessCanBeReconciled() {
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentProviderRegistry registry = mock(PaymentProviderRegistry.class);
        PaymentTransactionService transactionService = mock(PaymentTransactionService.class);
        PaymentProvider provider = mock(PaymentProvider.class);
        PaymentRuntimeProperties properties = new PaymentRuntimeProperties();
        ProviderCallbackResult result = new ProviderCallbackResult();
        Payment expired = new Payment();
        expired.setId(73L);
        expired.setStatus(PaymentStatus.EXPIRED);
        expired.setProviderCode(ProviderCode.VNPAY);

        when(repository.findByStatusAndSettlementHoldUntilBefore(
                eq(PaymentStatus.PROCESSING), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(repository.findByStatusAndSettlementHoldUntilBefore(
                eq(PaymentStatus.EXPIRED), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(expired)));
        when(registry.getProvider(ProviderCode.VNPAY)).thenReturn(provider);
        when(provider.queryStatus(expired)).thenReturn(Optional.of(result));

        PaymentProviderRecoveryScheduler scheduler = new PaymentProviderRecoveryScheduler(
                repository, registry, transactionService, properties);
        scheduler.recoverUncertainProviderSessions();

        verify(provider).queryStatus(expired);
        verify(transactionService).applyProviderResult(
                ProviderCode.VNPAY, result, null);
    }
}
