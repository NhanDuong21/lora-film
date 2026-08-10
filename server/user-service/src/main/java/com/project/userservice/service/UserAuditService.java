package com.project.userservice.service;

import com.project.userservice.entity.UserAuditLog;
import com.project.userservice.mapper.UserAuditMapper;
import com.project.userservice.repository.UserAuditLogRepository;
import com.project.userservice.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuditService {
    private final UserAuditLogRepository repository;
    private final UserAuditMapper userAuditMapper;

    public UserAuditService(UserAuditLogRepository repository, UserAuditMapper userAuditMapper) {
        this.repository = repository;
        this.userAuditMapper = userAuditMapper;
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

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.project.userservice.dto.response.UserAuditResponse> search(
            String keyword, String targetType, org.springframework.data.domain.Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String normalizedType = targetType == null || targetType.isBlank()
                ? null : targetType.trim().toUpperCase(java.util.Locale.ROOT);
        org.springframework.data.domain.Pageable safe = org.springframework.data.domain.PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), 100),
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return repository.search(normalizedKeyword, normalizedType, safe)
                .map(userAuditMapper::toResponse);
    }
}
