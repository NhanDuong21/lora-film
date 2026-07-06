package com.project.paymentservice.service;

import com.project.paymentservice.enumtype.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class PaymentStateTransitionService {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.PENDING, Set.of(
                    PaymentStatus.PROCESSING,
                    PaymentStatus.SUCCESS,
                    PaymentStatus.FAILED,
                    PaymentStatus.CANCELLED,
                    PaymentStatus.EXPIRED),
            PaymentStatus.PROCESSING, Set.of(
                    PaymentStatus.SUCCESS,
                    PaymentStatus.FAILED,
                    PaymentStatus.EXPIRED),
            PaymentStatus.CANCELLED, Set.of(PaymentStatus.SUCCESS),
            PaymentStatus.EXPIRED, Set.of(PaymentStatus.SUCCESS)
    );

    public boolean isTransitionAllowed(PaymentStatus from, PaymentStatus to) {
        Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELLED
                || status == PaymentStatus.EXPIRED;
    }

    public boolean isActive(PaymentStatus status) {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }

    public boolean isLateSuccess(PaymentStatus currentStatus) {
        return currentStatus == PaymentStatus.CANCELLED || currentStatus == PaymentStatus.EXPIRED;
    }
}
