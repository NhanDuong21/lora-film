package com.project.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.entity.EmailProviderConfiguration;
import com.project.notificationservice.entity.NotificationAuditLog;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.provider.SmtpMailSenderFactory;
import com.project.notificationservice.repository.EmailProviderConfigurationRepository;
import com.project.notificationservice.repository.NotificationAuditLogRepository;
import jakarta.mail.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailProviderConfigurationServiceTest {

    private EmailProviderConfigurationRepository repository;
    private NotificationAuditLogRepository auditRepository;
    private RecipientCryptoService cryptoService;
    private SmtpMailSenderFactory senderFactory;
    private JavaMailSender fallbackSender;
    private JavaMailSender replacementSender;
    private EmailProviderConfigurationService service;

    @BeforeEach
    void setUp() {
        repository = mock(EmailProviderConfigurationRepository.class);
        auditRepository = mock(NotificationAuditLogRepository.class);
        cryptoService = mock(RecipientCryptoService.class);
        senderFactory = mock(SmtpMailSenderFactory.class);
        fallbackSender = mock(JavaMailSender.class);
        replacementSender = mock(JavaMailSender.class);

        when(repository.findByConfigKey(EmailProviderConfiguration.PRIMARY_CONFIG_KEY))
                .thenReturn(Optional.empty());
        when(cryptoService.encrypt(any())).thenReturn("encrypted-secret");
        when(senderFactory.create(any())).thenReturn(replacementSender);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            EmailProviderConfiguration configuration = invocation.getArgument(0);
            configuration.beforeInsert();
            return configuration;
        });

        service = new EmailProviderConfigurationService(
                repository,
                auditRepository,
                cryptoService,
                senderFactory,
                new ObjectMapper(),
                fallbackSender,
                "smtp.gmail.com",
                587,
                "old@example.com",
                "old-password",
                "notifications@example.com",
                "LoraFilm",
                true,
                true,
                true);
        service.initialize();
    }

    @Test
    void testsEncryptsAuditsAndActivatesNewSender() {
        EmailProviderConfigurationService.EmailProviderStatus status = service.update(
                " New.Sender@Example.com ",
                "abcd efgh ijkl mnop",
                "LoraFilm Cinema",
                "admin-1");

        ArgumentCaptor<EmailProviderConfiguration> configurationCaptor =
                ArgumentCaptor.forClass(EmailProviderConfiguration.class);
        verify(repository).saveAndFlush(configurationCaptor.capture());
        EmailProviderConfiguration stored = configurationCaptor.getValue();
        assertThat(stored.getSenderEmail()).isEqualTo("new.sender@example.com");
        assertThat(stored.getAppPasswordEncrypted()).isEqualTo("encrypted-secret");
        verify(cryptoService).encrypt("abcdefghijklmnop");

        ArgumentCaptor<NotificationAuditLog> auditCaptor =
                ArgumentCaptor.forClass(NotificationAuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getMetadataJson())
                .contains("new.sender@example.com")
                .doesNotContain("abcdefghijklmnop")
                .doesNotContain("encrypted-secret");

        assertThat(status.source()).isEqualTo("ADMIN");
        assertThat(status.passwordConfigured()).isTrue();
        assertThat(service.currentSender().mailSender()).isSameAs(replacementSender);
        assertThat(service.currentSender().senderEmail()).isEqualTo("new.sender@example.com");
    }

    @Test
    void rejectedCredentialsDoNotReplaceOrPersistCurrentSender() throws Exception {
        doThrow(new AuthenticationFailedException("bad credentials"))
                .when(senderFactory).testConnection(any());

        assertThatThrownBy(() -> service.update(
                "new@example.com", "invalid-password", "LoraFilm", "admin-1"))
                .isInstanceOf(NotificationException.class)
                .extracting(exception -> ((NotificationException) exception).getErrorCode())
                .isEqualTo("SMTP_AUTHENTICATION_FAILED");

        verify(repository, never()).saveAndFlush(any());
        verify(auditRepository, never()).save(any());
        assertThat(service.currentSender().mailSender()).isSameAs(fallbackSender);
        assertThat(service.currentSender().source()).isEqualTo("ENV");
    }
}
