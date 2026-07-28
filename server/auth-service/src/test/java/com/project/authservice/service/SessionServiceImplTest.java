package com.project.authservice.service;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.UserSession;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.UserSessionRepository;
import com.project.authservice.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionServiceImplTest {
    @Test
    void exposesStoredDeviceAndActivityMetadata() {
        UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        CredentialRevocationService revocationService = mock(CredentialRevocationService.class);
        SessionServiceImpl service = new SessionServiceImpl(
                sessionRepository, accountRepository, revocationService);
        Account account = new Account();
        account.setId(18L);
        LocalDateTime lastActiveAt = LocalDateTime.of(2026, 7, 28, 11, 45);
        UserSession session = UserSession.builder()
                .id(7L)
                .account(account)
                .deviceName("Chrome on Windows")
                .ipAddress("127.0.0.1")
                .userAgent("Chrome")
                .lastActiveAt(lastActiveAt)
                .expiresAt(lastActiveAt.plusDays(7))
                .build();
        when(accountRepository.findByEmail("employee@example.com"))
                .thenReturn(Optional.of(account));
        when(sessionRepository.findByAccountIdAndIsOnlineTrue(18L))
                .thenReturn(List.of(session));

        var response = service.getUserSessions("employee@example.com");

        assertThat(response).singleElement().satisfies(value -> {
            assertThat(value.getDeviceName()).isEqualTo("Chrome on Windows");
            assertThat(value.getLastActiveAt()).isEqualTo(lastActiveAt);
        });
    }
}
