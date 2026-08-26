package com.project.authservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.entity.Account;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.CredentialRevocationService;
import com.project.authservice.service.EmployeeInvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLifecycleConsumerTest {
    @Mock AccountRepository accountRepository;
    @Mock CredentialRevocationService revocationService;
    @Mock RoleRepository roleRepository;
    @Mock EmployeeInvitationService invitationService;

    private UserLifecycleConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UserLifecycleConsumer(new ObjectMapper(), accountRepository,
                revocationService, roleRepository, invitationService);
    }

    @Test
    void cancellingOnboardingDisablesAccountAndInvalidatesInvitation() throws Exception {
        Account account = account(51L);
        when(accountRepository.findById(51L)).thenReturn(Optional.of(account));

        consumer.consume("""
                {"eventType":"EMPLOYEE_ONBOARDING_CANCELLED","data":{"accountId":51}}
                """);

        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(account.getIsEnabled()).isFalse();
        verify(accountRepository).save(account);
        verify(invitationService).invalidate(51L);
        verify(revocationService).revokeAll(51L);
    }

    @Test
    void reopeningOnboardingReusesAccountAndIssuesANewInvitation() throws Exception {
        Account account = account(52L);
        when(accountRepository.findById(52L)).thenReturn(Optional.of(account));

        consumer.consume("""
                {"eventType":"EMPLOYEE_ONBOARDING_REOPENED","data":{"accountId":52}}
                """);

        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(account.getIsEnabled()).isFalse();
        verify(accountRepository).save(account);
        verify(invitationService).issue(account, null);
        verify(revocationService).revokeAll(52L);
    }

    private Account account(Long id) {
        return Account.builder()
                .id(id)
                .email("employee@example.com")
                .passwordHash("unavailable")
                .status(AccountStatus.ACTIVE)
                .isEnabled(true)
                .isDeleted(false)
                .build();
    }
}
