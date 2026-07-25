package com.project.promotionservice.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditable)")
    public Object profile(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            AuditLog auditLog = new AuditLog();
            auditLog.setAction(auditable.action());
            auditLog.setEntityType(auditable.entityType().isEmpty() ? joinPoint.getTarget().getClass().getSimpleName() : auditable.entityType());

            // Extract entity public ID if present in method arguments or return value
            String entityPublicId = "N/A";
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                try {
                    auditLog.setBeforeData(objectMapper.writeValueAsString(args[0]));
                } catch (Exception ex) {
                    log.warn("Failed to serialize method arguments for audit log", ex);
                }
            }

            if (result != null) {
                try {
                    auditLog.setAfterData(objectMapper.writeValueAsString(result));
                    // Try to reflectively get publicId
                    java.lang.reflect.Method getPublicIdMethod = result.getClass().getMethod("getPublicId");
                    entityPublicId = (String) getPublicIdMethod.invoke(result);
                } catch (NoSuchMethodException e) {
                    // Ignore, entityPublicId remains N/A
                } catch (Exception ex) {
                    log.warn("Failed to extract publicId from result", ex);
                }
            }
            auditLog.setEntityPublicId(entityPublicId);

            // User Info
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                auditLog.setActorPublicId(principal.getUserId() != null ? principal.getUserId().toString() : "anonymous");
                auditLog.setActorType("USER");
                auditLog.setCreatedBy(principal.getUserId() != null ? principal.getUserId().toString() : "anonymous");
            } else {
                auditLog.setActorPublicId("anonymous");
                auditLog.setActorType("SYSTEM");
                auditLog.setCreatedBy("system");
            }

            // HTTP Info
            if (request != null) {
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }

            // Trace & Request
            auditLog.setTraceId(MDC.get("traceId"));
            auditLog.setRequestId(MDC.get("correlationId"));

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
