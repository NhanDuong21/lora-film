package com.project.promotionservice.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.json.SensitiveDataSanitizer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogTest {

    @Test
    void initializesRequiredTimestamps() {
        AuditLog auditLog = new AuditLog();

        assertThat(auditLog.getCreatedAt()).isNotNull();
        assertThat(auditLog.getUpdatedAt()).isEqualTo(auditLog.getCreatedAt());
    }

    @Test
    void recordsSystemActorAsSystemInsteadOfUser() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditTrailService service = new AuditTrailService(
                repository, new ObjectMapper(), new SensitiveDataSanitizer());

        service.record("PROMOTION_AUTOMATION_RUN", "run-1",
                "AUTOMATION_RUN_CREATE", null, java.util.Map.of("count", 3),
                "SYSTEM");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActorPublicId()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().getActorType()).isEqualTo("SYSTEM");
    }
}
