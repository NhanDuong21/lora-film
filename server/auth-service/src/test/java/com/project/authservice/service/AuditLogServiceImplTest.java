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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;

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

        verify(writer, never()).write(eq(18L), isNull(), eq("PASSWORD_CHANGED"), eq("ACCOUNT"),
                eq("18"), isNull(), eq("SUCCESS"), eq("NORMAL"), eq("NOT_REQUIRED"),
                eq("127.0.0.1"), eq("JUnit"));
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }
        verify(writer).write(eq(18L), isNull(), eq("PASSWORD_CHANGED"), eq("ACCOUNT"),
                eq("18"), isNull(), eq("SUCCESS"), eq("NORMAL"), eq("NOT_REQUIRED"),
                eq("127.0.0.1"), eq("JUnit"));
    }

    @Test
    void writesImmediatelyWhenNoBusinessTransactionExists() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        service.log(null, "REGISTER_FAILED", request);

        verify(writer).write(isNull(), isNull(), eq("REGISTER_FAILED"), eq("ACCOUNT"),
                isNull(), isNull(), eq("FAILED"), eq("NORMAL"), eq("NOT_REQUIRED"),
                eq("127.0.0.1"), isNull());
    }
}
