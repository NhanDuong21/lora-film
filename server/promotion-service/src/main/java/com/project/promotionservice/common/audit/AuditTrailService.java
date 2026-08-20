package com.project.promotionservice.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.json.SensitiveDataSanitizer;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTrailService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final SensitiveDataSanitizer sanitizer;

    public AuditTrailService(AuditLogRepository repository,
                             ObjectMapper objectMapper,
                             SensitiveDataSanitizer sanitizer) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.sanitizer = sanitizer;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, String entityPublicId, String action,
                       Object before, Object after, String actor) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityPublicId(entityPublicId);
        log.setAction(action);
        log.setActorPublicId(actor);
        log.setActorType(actorType(actor));
        log.setCreatedBy(actor);
        log.setRequestId(MDC.get("correlationId"));
        log.setTraceId(MDC.get("traceId"));
        log.setBeforeData(toJson(before));
        log.setAfterData(toJson(after));
        repository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInNewTransaction(String entityType, String entityPublicId, String action,
                                        Object before, Object after, String actor) {
        recordInternal(entityType, entityPublicId, action, before, after, actor);
    }

    private void recordInternal(String entityType, String entityPublicId, String action,
                                Object before, Object after, String actor) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityPublicId(entityPublicId);
        log.setAction(action);
        log.setActorPublicId(actor);
        log.setActorType(actorType(actor));
        log.setCreatedBy(actor);
        log.setRequestId(MDC.get("correlationId"));
        log.setTraceId(MDC.get("traceId"));
        log.setBeforeData(toJson(before));
        log.setAfterData(toJson(after));
        repository.save(log);
    }

    private String actorType(String actor) {
        if (actor == null || "SYSTEM".equalsIgnoreCase(actor)) {
            return "SYSTEM";
        }
        return actor.endsWith("_SERVICE") ? "SERVICE" : "USER";
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(
                    sanitizer.sanitize(objectMapper.valueToTree(value)));
        } catch (Exception exception) {
            return "{\"serialization\":\"unavailable\"}";
        }
    }
}
