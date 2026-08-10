package com.project.authservice.service;

import com.project.authservice.repository.AuditLogRepository;
import com.project.authservice.service.impl.AuditLogServiceImpl;
import com.project.authservice.service.impl.AuditLogWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceImplTest {
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditLogWriter writer = mock(AuditLogWriter.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final AuditLogServiceImpl service = new AuditLogServiceImpl(repository, writer);

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void defersAuditWriteUntilBusinessTransactionReleasesLocks() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        service.log(18L, "PASSWORD_CHANGED", request);

        verify(writer, never()).write(18L, "PASSWORD_CHANGED", "ACCOUNT", "127.0.0.1", "JUnit");
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }
        verify(writer).write(18L, "PASSWORD_CHANGED", "ACCOUNT", "127.0.0.1", "JUnit");
    }

    @Test
    void writesImmediatelyWhenNoBusinessTransactionExists() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        service.log(null, "REGISTER_FAILED", request);

        verify(writer).write(null, "REGISTER_FAILED", "ACCOUNT", "127.0.0.1", null);
    }
}
