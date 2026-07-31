package com.project.promotionservice.common.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogTest {

    @Test
    void initializesRequiredTimestamps() {
        AuditLog auditLog = new AuditLog();

        assertThat(auditLog.getCreatedAt()).isNotNull();
        assertThat(auditLog.getUpdatedAt()).isEqualTo(auditLog.getCreatedAt());
    }
}
