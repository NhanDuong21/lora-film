package com.project.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "notification_email_provider_configs")
public class EmailProviderConfiguration {

    public static final String PRIMARY_CONFIG_KEY = "SMTP_PRIMARY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "config_key", nullable = false, unique = true, length = 40)
    private String configKey;
    @Column(name = "smtp_host", nullable = false, length = 255)
    private String smtpHost;
    @Column(name = "smtp_port", nullable = false)
    private int smtpPort;
    @Column(name = "sender_email", nullable = false, length = 320)
    private String senderEmail;
    @Column(name = "app_password_encrypted", nullable = false, length = 1000)
    private String appPasswordEncrypted;
    @Column(name = "from_name", nullable = false, length = 120)
    private String fromName;
    @Column(name = "smtp_auth_enabled", nullable = false)
    private boolean smtpAuthEnabled;
    @Column(name = "starttls_enabled", nullable = false)
    private boolean starttlsEnabled;
    @Column(name = "starttls_required", nullable = false)
    private boolean starttlsRequired;
    @Column(name = "connection_status", nullable = false, length = 30)
    private String connectionStatus;
    @Column(name = "last_tested_at")
    private Instant lastTestedAt;
    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();
        if (configKey == null) configKey = PRIMARY_CONFIG_KEY;
        if (connectionStatus == null) connectionStatus = "CONNECTED";
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String value) { this.configKey = value; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String value) { this.smtpHost = value; }
    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int value) { this.smtpPort = value; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String value) { this.senderEmail = value; }
    public String getAppPasswordEncrypted() { return appPasswordEncrypted; }
    public void setAppPasswordEncrypted(String value) { this.appPasswordEncrypted = value; }
    public String getFromName() { return fromName; }
    public void setFromName(String value) { this.fromName = value; }
    public boolean isSmtpAuthEnabled() { return smtpAuthEnabled; }
    public void setSmtpAuthEnabled(boolean value) { this.smtpAuthEnabled = value; }
    public boolean isStarttlsEnabled() { return starttlsEnabled; }
    public void setStarttlsEnabled(boolean value) { this.starttlsEnabled = value; }
    public boolean isStarttlsRequired() { return starttlsRequired; }
    public void setStarttlsRequired(boolean value) { this.starttlsRequired = value; }
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String value) { this.connectionStatus = value; }
    public Instant getLastTestedAt() { return lastTestedAt; }
    public void setLastTestedAt(Instant value) { this.lastTestedAt = value; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String value) { this.updatedBy = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    @Override
    public String toString() {
        return "EmailProviderConfiguration{configKey='" + configKey
                + "', smtpHost='" + smtpHost + "', smtpPort=" + smtpPort
                + ", senderEmail='" + senderEmail + "', connectionStatus='"
                + connectionStatus + "'}";
    }
}
