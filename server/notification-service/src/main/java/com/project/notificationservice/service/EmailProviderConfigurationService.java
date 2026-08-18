package com.project.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.entity.EmailProviderConfiguration;
import com.project.notificationservice.entity.NotificationAuditLog;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.provider.SmtpMailSenderFactory;
import com.project.notificationservice.provider.SmtpMailSenderFactory.SmtpSettings;
import com.project.notificationservice.repository.EmailProviderConfigurationRepository;
import com.project.notificationservice.repository.NotificationAuditLogRepository;
import jakarta.annotation.PostConstruct;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class EmailProviderConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(EmailProviderConfigurationService.class);

    private final EmailProviderConfigurationRepository repository;
    private final NotificationAuditLogRepository auditRepository;
    private final RecipientCryptoService cryptoService;
    private final SmtpMailSenderFactory senderFactory;
    private final ObjectMapper objectMapper;
    private final JavaMailSender fallbackMailSender;
    private final SmtpSettings fallbackSettings;
    private final String fallbackFromAddress;
    private final AtomicReference<ActiveEmailSender> activeSender = new AtomicReference<>();

    public EmailProviderConfigurationService(
            EmailProviderConfigurationRepository repository,
            NotificationAuditLogRepository auditRepository,
            RecipientCryptoService cryptoService,
            SmtpMailSenderFactory senderFactory,
            ObjectMapper objectMapper,
            JavaMailSender fallbackMailSender,
            @Value("${spring.mail.host:smtp.gmail.com}") String fallbackHost,
            @Value("${spring.mail.port:587}") int fallbackPort,
            @Value("${spring.mail.username:}") String fallbackEmail,
            @Value("${spring.mail.password:}") String fallbackPassword,
            @Value("${notification.delivery.from-address:${spring.mail.username:}}") String fallbackFromAddress,
            @Value("${notification.delivery.from-name:LoraFilm}") String fallbackFromName,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean fallbackAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean fallbackStarttls,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:true}") boolean fallbackStarttlsRequired) {
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.cryptoService = cryptoService;
        this.senderFactory = senderFactory;
        this.objectMapper = objectMapper;
        this.fallbackMailSender = fallbackMailSender;
        this.fallbackSettings = new SmtpSettings(
                fallbackHost, fallbackPort, fallbackEmail, fallbackPassword,
                fallbackFromName, fallbackAuth, fallbackStarttls, fallbackStarttlsRequired);
        this.fallbackFromAddress = fallbackFromAddress;
    }

    @PostConstruct
    public void initialize() {
        ActiveEmailSender fallback = new ActiveEmailSender(
                fallbackMailSender,
                fallbackFromAddress,
                fallbackSettings.fromName(),
                "ENV");
        activeSender.set(fallback);
        try {
            repository.findByConfigKey(EmailProviderConfiguration.PRIMARY_CONFIG_KEY)
                    .ifPresent(configuration -> {
                        SmtpSettings settings = settings(configuration,
                                cryptoService.decrypt(configuration.getAppPasswordEncrypted()));
                        activeSender.set(active(settings, "ADMIN"));
                        log.info("Loaded admin-managed SMTP configuration sender={}",
                                maskEmail(configuration.getSenderEmail()));
                    });
        } catch (DataAccessException | NotificationException exception) {
            log.error("Could not load admin-managed SMTP configuration; environment sender remains active code={}",
                    exception instanceof NotificationException notificationException
                            ? notificationException.getErrorCode() : "SMTP_CONFIG_LOAD_FAILED");
        }
    }

    public ActiveEmailSender currentSender() {
        return activeSender.get();
    }

    @Transactional(readOnly = true)
    public EmailProviderStatus status() {
        Optional<EmailProviderConfiguration> stored = repository.findByConfigKey(
                EmailProviderConfiguration.PRIMARY_CONFIG_KEY);
        ActiveEmailSender current = activeSender.get();
        if (stored.isPresent() && "ADMIN".equals(current.source())) {
            return toStatus(stored.get(), true);
        }
        boolean environmentConfigured = fallbackSettings.senderEmail() != null
                && !fallbackSettings.senderEmail().isBlank()
                && fallbackFromAddress != null
                && !fallbackFromAddress.isBlank()
                && fallbackSettings.appPassword() != null
                && !fallbackSettings.appPassword().isBlank();
        return new EmailProviderStatus(
                environmentConfigured,
                "ENV",
                fallbackFromAddress,
                maskEmail(fallbackFromAddress),
                fallbackSettings.fromName(),
                fallbackSettings.host(),
                fallbackSettings.port(),
                fallbackSettings.smtpAuthEnabled(),
                fallbackSettings.starttlsEnabled(),
                fallbackSettings.starttlsRequired(),
                fallbackSettings.appPassword() != null && !fallbackSettings.appPassword().isBlank(),
                "CHƯA KIỂM TRA",
                null,
                null,
                "environment");
    }

    public SmtpConnectionTestResult testConnection(
            String senderEmail,
            String appPassword,
            String fromName) {
        SmtpSettings candidate = candidate(senderEmail, appPassword, fromName);
        verifyConnection(candidate);
        return new SmtpConnectionTestResult(
                true,
                "Kết nối SMTP và App Password hợp lệ.",
                candidate.host(),
                candidate.port(),
                maskEmail(candidate.senderEmail()),
                Instant.now());
    }

    @Transactional
    public EmailProviderStatus update(
            String senderEmail,
            String appPassword,
            String fromName,
            String actorPublicId) {
        SmtpSettings candidate = candidate(senderEmail, appPassword, fromName);
        verifyConnection(candidate);

        EmailProviderConfiguration configuration = repository
                .findByConfigKey(EmailProviderConfiguration.PRIMARY_CONFIG_KEY)
                .orElseGet(EmailProviderConfiguration::new);
        configuration.setConfigKey(EmailProviderConfiguration.PRIMARY_CONFIG_KEY);
        configuration.setSmtpHost(candidate.host());
        configuration.setSmtpPort(candidate.port());
        configuration.setSenderEmail(candidate.senderEmail());
        configuration.setAppPasswordEncrypted(cryptoService.encrypt(candidate.appPassword()));
        configuration.setFromName(candidate.fromName());
        configuration.setSmtpAuthEnabled(candidate.smtpAuthEnabled());
        configuration.setStarttlsEnabled(candidate.starttlsEnabled());
        configuration.setStarttlsRequired(candidate.starttlsRequired());
        configuration.setConnectionStatus("CONNECTED");
        configuration.setLastTestedAt(Instant.now());
        configuration.setUpdatedBy(normalizeActor(actorPublicId));
        configuration = repository.saveAndFlush(configuration);
        audit(configuration, actorPublicId);

        ActiveEmailSender replacement = active(candidate, "ADMIN");
        activateAfterCommit(replacement);
        return toStatus(configuration, true);
    }

    private SmtpSettings candidate(String senderEmail, String appPassword, String fromName) {
        String normalizedEmail = senderEmail == null
                ? "" : senderEmail.trim().toLowerCase(java.util.Locale.ROOT);
        String password = appPassword == null
                ? "" : appPassword.replaceAll("\\s+", "");
        String normalizedFromName = fromName == null || fromName.isBlank()
                ? "LoraFilm" : fromName.trim();
        SmtpSettings current = currentBaseSettings();
        return new SmtpSettings(
                current.host(),
                current.port(),
                normalizedEmail,
                password,
                normalizedFromName,
                current.smtpAuthEnabled(),
                current.starttlsEnabled(),
                current.starttlsRequired());
    }

    private SmtpSettings currentBaseSettings() {
        return repository.findByConfigKey(EmailProviderConfiguration.PRIMARY_CONFIG_KEY)
                .map(configuration -> settings(configuration, ""))
                .orElse(fallbackSettings);
    }

    private SmtpSettings settings(EmailProviderConfiguration configuration, String password) {
        return new SmtpSettings(
                configuration.getSmtpHost(),
                configuration.getSmtpPort(),
                configuration.getSenderEmail(),
                password,
                configuration.getFromName(),
                configuration.isSmtpAuthEnabled(),
                configuration.isStarttlsEnabled(),
                configuration.isStarttlsRequired());
    }

    private void verifyConnection(SmtpSettings candidate) {
        try {
            senderFactory.testConnection(candidate);
        } catch (MessagingException | RuntimeException exception) {
            throw connectionFailure(exception);
        }
    }

    private NotificationException connectionFailure(Throwable exception) {
        if (findCause(exception, AuthenticationFailedException.class) != null) {
            return new NotificationException(
                    "SMTP_AUTHENTICATION_FAILED",
                    "Email hoặc App Password không hợp lệ. Gmail đã từ chối đăng nhập SMTP.",
                    HttpStatus.BAD_REQUEST);
        }
        if (findCause(exception, UnknownHostException.class) != null
                || findCause(exception, ConnectException.class) != null
                || findCause(exception, SocketTimeoutException.class) != null) {
            return new NotificationException(
                    "SMTP_CONNECTION_FAILED",
                    "Không thể kết nối máy chủ SMTP. Vui lòng kiểm tra mạng và thử lại.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new NotificationException(
                "SMTP_CONFIGURATION_TEST_FAILED",
                "Không thể xác nhận cấu hình SMTP. Kiểm tra email, App Password và trạng thái tài khoản gửi.",
                HttpStatus.BAD_REQUEST);
    }

    private <T extends Throwable> T findCause(Throwable exception, Class<T> type) {
        Throwable current = exception;
        for (int depth = 0; current != null && depth < 15; depth++) {
            if (type.isInstance(current)) return type.cast(current);
            current = current.getCause();
        }
        return null;
    }

    private ActiveEmailSender active(SmtpSettings settings, String source) {
        return new ActiveEmailSender(
                senderFactory.create(settings),
                settings.senderEmail(),
                settings.fromName(),
                source);
    }

    private void activateAfterCommit(ActiveEmailSender replacement) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    activeSender.set(replacement);
                }
            });
        } else {
            activeSender.set(replacement);
        }
    }

    private void audit(EmailProviderConfiguration configuration, String actorPublicId) {
        NotificationAuditLog audit = new NotificationAuditLog();
        audit.setActorPublicId(normalizeActor(actorPublicId));
        audit.setAction("UPDATE_SMTP_CONFIGURATION");
        audit.setTargetType("EMAIL_PROVIDER_CONFIGURATION");
        audit.setTargetPublicId(EmailProviderConfiguration.PRIMARY_CONFIG_KEY);
        audit.setMetadataJson(writeMetadata(Map.of(
                "senderEmail", configuration.getSenderEmail(),
                "smtpHost", configuration.getSmtpHost(),
                "smtpPort", configuration.getSmtpPort(),
                "fromName", configuration.getFromName(),
                "connectionStatus", configuration.getConnectionStatus())));
        auditRepository.save(audit);
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            return "{\"result\":\"metadata_unavailable\"}";
        }
    }

    private EmailProviderStatus toStatus(
            EmailProviderConfiguration configuration,
            boolean managedByAdmin) {
        return new EmailProviderStatus(
                true,
                managedByAdmin ? "ADMIN" : "ENV",
                configuration.getSenderEmail(),
                maskEmail(configuration.getSenderEmail()),
                configuration.getFromName(),
                configuration.getSmtpHost(),
                configuration.getSmtpPort(),
                configuration.isSmtpAuthEnabled(),
                configuration.isStarttlsEnabled(),
                configuration.isStarttlsRequired(),
                configuration.getAppPasswordEncrypted() != null
                        && !configuration.getAppPasswordEncrypted().isBlank(),
                configuration.getConnectionStatus(),
                configuration.getLastTestedAt(),
                configuration.getUpdatedAt(),
                configuration.getUpdatedBy());
    }

    private String normalizeActor(String actorPublicId) {
        return actorPublicId == null || actorPublicId.isBlank()
                ? "system" : actorPublicId.trim();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "—";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.isEmpty() ? "*" : local.substring(0, 1) + "***";
        return masked + "@" + parts[1];
    }

    public static final class ActiveEmailSender {
        private final JavaMailSender mailSender;
        private final String senderEmail;
        private final String fromName;
        private final String source;

        public ActiveEmailSender(
                JavaMailSender mailSender,
                String senderEmail,
                String fromName,
                String source) {
            this.mailSender = mailSender;
            this.senderEmail = senderEmail;
            this.fromName = fromName;
            this.source = source;
        }

        public JavaMailSender mailSender() { return mailSender; }
        public String senderEmail() { return senderEmail; }
        public String fromName() { return fromName; }
        public String source() { return source; }
    }

    public record EmailProviderStatus(
            boolean configured,
            String source,
            String senderEmail,
            String senderEmailMasked,
            String fromName,
            String smtpHost,
            int smtpPort,
            boolean smtpAuthEnabled,
            boolean starttlsEnabled,
            boolean starttlsRequired,
            boolean passwordConfigured,
            String connectionStatus,
            Instant lastTestedAt,
            Instant updatedAt,
            String updatedBy) {
    }

    public record SmtpConnectionTestResult(
            boolean connected,
            String message,
            String smtpHost,
            int smtpPort,
            String senderEmailMasked,
            Instant testedAt) {
    }
}
