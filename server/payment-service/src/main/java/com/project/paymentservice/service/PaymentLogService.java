package com.project.paymentservice.service;

import com.project.paymentservice.entity.PaymentLog;
import com.project.paymentservice.enumtype.ActorType;
import com.project.paymentservice.enumtype.PaymentLogEventType;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.repository.PaymentLogRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentLogService {

    private final PaymentLogRepository paymentLogRepository;

    public PaymentLogService(PaymentLogRepository paymentLogRepository) {
        this.paymentLogRepository = paymentLogRepository;
    }

    public void log(Long paymentId, PaymentLogEventType eventType, String source,
                    ActorType actorType, Long actorAccountId,
                    PaymentStatus previousStatus, PaymentStatus currentStatus,
                    String message, String metadata) {
        PaymentLog log = new PaymentLog();
        log.setPaymentId(paymentId);
        log.setEventType(eventType);
        log.setSource(source);
        log.setActorType(actorType);
        log.setActorAccountId(actorAccountId);
        log.setPreviousStatus(previousStatus);
        log.setCurrentStatus(currentStatus);
        log.setMessageSanitized(message);
        log.setMetadataSanitized(metadata);
        paymentLogRepository.save(log);
    }
}
