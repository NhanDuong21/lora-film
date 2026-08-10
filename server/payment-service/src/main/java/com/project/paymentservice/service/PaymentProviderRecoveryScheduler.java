package com.project.paymentservice.service;

import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PaymentProviderRecoveryScheduler {
    private static final Logger log = LoggerFactory.getLogger(PaymentProviderRecoveryScheduler.class);

    private final PaymentRepository paymentRepository;
    private final PaymentProviderRegistry providerRegistry;
    private final PaymentTransactionService transactionService;
    private final PaymentRuntimeProperties properties;

    public PaymentProviderRecoveryScheduler(
            PaymentRepository paymentRepository,
            PaymentProviderRegistry providerRegistry,
            PaymentTransactionService transactionService,
            PaymentRuntimeProperties properties) {
        this.paymentRepository = paymentRepository;
        this.providerRegistry = providerRegistry;
        this.transactionService = transactionService;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${payment.runtime.provider-recovery-fixed-delay-millis:5000}",
            initialDelayString = "${payment.runtime.provider-recovery-initial-delay-millis:5000}")
    public void recoverUncertainProviderSessions() {
        Instant now = Instant.now();
        List<Payment> due = new ArrayList<>();
        due.addAll(paymentRepository.findByStatusAndSettlementHoldUntilBefore(
                PaymentStatus.PROCESSING, now, PageRequest.of(0, 20)).getContent());
        due.addAll(paymentRepository.findByStatusAndSettlementHoldUntilBefore(
                PaymentStatus.EXPIRED, now, PageRequest.of(0, 20)).getContent());
        for (Payment payment : due) {
            PaymentProvider provider;
            try {
                provider = providerRegistry.getProvider(payment.getProviderCode());
            } catch (RuntimeException exception) {
                transactionService.deferUncertainStatus(
                        payment.getId(), now.plusSeconds(properties.getSettlementHoldSeconds()));
                continue;
            }
            Optional<ProviderCallbackResult> result;
            try {
                result = provider.queryStatus(payment);
            } catch (RuntimeException exception) {
                defer(payment, provider, now);
                log.warn("Provider status query failed: paymentId={}, provider={}, error={}",
                        payment.getId(), payment.getProviderCode(), exception.getClass().getSimpleName());
                continue;
            }
            if (result.isPresent()) {
                try {
                    transactionService.applyProviderResult(
                            payment.getProviderCode(), result.get(), null);
                } catch (BusinessException exception) {
                    defer(payment, provider, now);
                    log.warn("Provider recovery result requires attention: paymentId={}, provider={}, errorCode={}",
                            payment.getId(), payment.getProviderCode(), exception.getErrorCode());
                } catch (RuntimeException exception) {
                    defer(payment, provider, now);
                    log.warn("Provider recovery result failed: paymentId={}, provider={}, error={}",
                            payment.getId(), payment.getProviderCode(), exception.getClass().getSimpleName());
                }
            } else {
                defer(payment, provider, now);
            }
        }
    }

    private void defer(Payment payment, PaymentProvider provider, Instant now) {
        int retryDelaySeconds = Math.max(
                properties.getSettlementHoldSeconds(),
                provider.recoveryRetryDelaySeconds());
        transactionService.deferUncertainStatus(
                payment.getId(), now.plusSeconds(retryDelaySeconds));
    }
}
