package com.project.paymentservice.service;

import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.ProviderRefundResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefundWorker {
    private static final Logger log = LoggerFactory.getLogger(RefundWorker.class);

    private final RefundService refundService;
    private final PaymentProviderRegistry providerRegistry;

    public RefundWorker(
            RefundService refundService,
            PaymentProviderRegistry providerRegistry) {
        this.refundService = refundService;
        this.providerRegistry = providerRegistry;
    }

    @Scheduled(
            fixedDelayString = "${payment.runtime.refund-fixed-delay-millis:3000}",
            initialDelayString = "${payment.runtime.refund-fixed-delay-millis:3000}")
    public void process() {
        String ownerToken = "refund-worker-" + UUID.randomUUID();
        for (Long refundId : refundService.claimReady(ownerToken)) {
            try {
                RefundService.RefundWork work = refundService.loadOwnedWork(refundId, ownerToken);
                PaymentProvider provider = providerRegistry.getProvider(
                        work.refund().getProviderCode());
                ProviderRefundResult result;
                if (work.queryOnly()) {
                    Optional<ProviderRefundResult> queried = provider.queryRefund(
                            work.payment(), work.refund());
                    if (queried.isEmpty()) {
                        refundService.markUncertain(
                                refundId, ownerToken,
                                "Nhà cung cấp chưa trả về trạng thái hoàn tiền xác định");
                        continue;
                    }
                    result = queried.get();
                } else {
                    refundService.markSubmitted(refundId, ownerToken);
                    result = provider.refund(work.payment(), work.refund());
                }
                refundService.applyProviderResult(refundId, ownerToken, result);
            } catch (Exception exception) {
                log.warn("Refund processing failed: refundId={}, error={}",
                        refundId, rootMessage(exception));
                try {
                    refundService.markUncertain(
                            refundId, ownerToken, rootMessage(exception));
                } catch (Exception leaseException) {
                    log.warn("Cannot reschedule refund {}: {}",
                            refundId, rootMessage(leaseException));
                }
            }
        }
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null
                ? current.getClass().getSimpleName() : current.getMessage();
    }
}
