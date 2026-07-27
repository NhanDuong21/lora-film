package com.project.userservice.service;

import com.project.userservice.entity.UserAuditLog;
import com.project.userservice.repository.UserAuditLogRepository;
import com.project.userservice.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuditService {
    private final UserAuditLogRepository repository;

    public UserAuditService(UserAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(String action, String targetType, Object targetId, String details) {
        UserAuditLog entry = new UserAuditLog();
        try {
            entry.setActorAccountId(CurrentActor.accountId());
        } catch (RuntimeException ignored) {
            entry.setActorAccountId(null);
        }
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId == null ? null : targetId.toString());
        entry.setDetails(details);
        repository.save(entry);
    }
}
