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
        String normalizedAction = action == null ? "" : action.toUpperCase(java.util.Locale.ROOT);
        boolean failed = normalizedAction.contains("FAILED") || normalizedAction.contains("REJECTED")
                || normalizedAction.contains("BLOCKED") || normalizedAction.contains("ERROR");
        boolean needsReview = java.util.Set.of("EMPLOYEE_RESIGNED", "PII_ERASED",
                "EMPLOYEE_DOCUMENT_DELETED", "PAYROLL_CANCELLED").contains(normalizedAction);
        entry.setResult(failed ? "FAILED" : "SUCCESS");
        entry.setSeverity(needsReview ? "REVIEW" : "NORMAL");
        entry.setReviewStatus(needsReview ? "UNREVIEWED" : "NOT_REQUIRED");
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.project.userservice.dto.response.UserAuditResponse> search(
            String keyword, String targetType, boolean attentionOnly,
            org.springframework.data.domain.Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String normalizedType = targetType == null || targetType.isBlank()
                ? null : targetType.trim().toUpperCase(java.util.Locale.ROOT);
        org.springframework.data.domain.Pageable safe = org.springframework.data.domain.PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), 100),
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return repository.search(normalizedKeyword, normalizedType, attentionOnly, safe)
                .map(userAuditMapper::toResponse);
    }

    @Transactional
    public com.project.userservice.dto.response.UserAuditResponse review(
            Long id, com.project.userservice.dto.request.UserAuditReviewRequest request) {
        UserAuditLog entry = repository.findById(id)
                .orElseThrow(() -> new com.project.userservice.exception.BusinessException(
                        "Không tìm thấy nhật ký", "USER_AUDIT_NOT_FOUND"));
        String status = request.status().trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("IN_PROGRESS", "REVIEWED", "DISMISSED", "RESOLVED").contains(status)) {
            throw new com.project.userservice.exception.BusinessException(
                    "Trạng thái rà soát không hợp lệ", "USER_AUDIT_REVIEW_INVALID");
        }
        String note = request.note() == null ? "" : request.note().trim();
        if (java.util.Set.of("DISMISSED", "RESOLVED").contains(status) && note.length() < 5) {
            throw new com.project.userservice.exception.BusinessException(
                    "Vui lòng ghi rõ kết quả xử lý trước khi hoàn tất", "USER_AUDIT_REVIEW_NOTE_REQUIRED");
        }
        entry.setReviewStatus(status);
        entry.setReviewedBy(CurrentActor.accountId());
        entry.setReviewNote(note.isBlank() ? null : note);
        entry.setReviewedAt(java.time.LocalDateTime.now());
        return userAuditMapper.toResponse(repository.save(entry));
    }
}
