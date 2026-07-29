package com.project.analyticsservice.application;

import com.project.analyticsservice.domain.service.FactIngestionDomainService;
import com.project.analyticsservice.domain.service.EventSourceMetadata;
import org.springframework.stereotype.Service;

@Service
public class EventIngestionApplicationService {
    private final FactIngestionDomainService domainService;

    public EventIngestionApplicationService(FactIngestionDomainService domainService) {
        this.domainService = domainService;
    }

    public void ingestPaymentSucceeded(String payload) {
        domainService.ingestPaymentSucceeded(payload);
    }

    public void ingestPaymentSucceeded(String payload, EventSourceMetadata metadata) {
        domainService.ingestPaymentSucceeded(payload, metadata);
    }

    public void ingestBookingCancelled(String payload) {
        domainService.ingestBookingCancelled(payload);
    }

    public void ingestBookingCancelled(String payload, EventSourceMetadata metadata) {
        domainService.ingestBookingCancelled(payload, metadata);
    }

    public void ingestPaymentRefunded(String payload) {
        domainService.ingestPaymentRefunded(payload);
    }

    public void ingestPaymentRefunded(String payload, EventSourceMetadata metadata) {
        domainService.ingestPaymentRefunded(payload, metadata);
    }
}
