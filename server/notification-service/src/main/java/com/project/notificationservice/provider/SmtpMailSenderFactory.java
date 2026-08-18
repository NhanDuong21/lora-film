package com.project.notificationservice.provider;

import jakarta.mail.MessagingException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class SmtpMailSenderFactory {

    public JavaMailSender create(SmtpSettings settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setUsername(settings.senderEmail());
        sender.setPassword(settings.appPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(settings.smtpAuthEnabled()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(settings.starttlsEnabled()));
        properties.put("mail.smtp.starttls.required", String.valueOf(settings.starttlsRequired()));
        properties.put("mail.smtp.connectiontimeout", "8000");
        properties.put("mail.smtp.timeout", "8000");
        properties.put("mail.smtp.writetimeout", "8000");
        return sender;
    }

    public void testConnection(SmtpSettings settings) throws MessagingException {
        ((JavaMailSenderImpl) create(settings)).testConnection();
    }

    public static final class SmtpSettings {
        private final String host;
        private final int port;
        private final String senderEmail;
        private final String appPassword;
        private final String fromName;
        private final boolean smtpAuthEnabled;
        private final boolean starttlsEnabled;
        private final boolean starttlsRequired;

        public SmtpSettings(
                String host,
                int port,
                String senderEmail,
                String appPassword,
                String fromName,
                boolean smtpAuthEnabled,
                boolean starttlsEnabled,
                boolean starttlsRequired) {
            this.host = host;
            this.port = port;
            this.senderEmail = senderEmail;
            this.appPassword = appPassword;
            this.fromName = fromName;
            this.smtpAuthEnabled = smtpAuthEnabled;
            this.starttlsEnabled = starttlsEnabled;
            this.starttlsRequired = starttlsRequired;
        }

        public String host() { return host; }
        public int port() { return port; }
        public String senderEmail() { return senderEmail; }
        public String appPassword() { return appPassword; }
        public String fromName() { return fromName; }
        public boolean smtpAuthEnabled() { return smtpAuthEnabled; }
        public boolean starttlsEnabled() { return starttlsEnabled; }
        public boolean starttlsRequired() { return starttlsRequired; }

        @Override
        public String toString() {
            return "SmtpSettings{host='" + host + "', port=" + port
                    + ", senderEmail='" + senderEmail + "', fromName='" + fromName
                    + "', smtpAuthEnabled=" + smtpAuthEnabled
                    + ", starttlsEnabled=" + starttlsEnabled
                    + ", starttlsRequired=" + starttlsRequired + "}";
        }
    }
}
