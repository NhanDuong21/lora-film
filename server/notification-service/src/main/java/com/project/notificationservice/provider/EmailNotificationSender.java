package com.project.notificationservice.provider;

import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class EmailNotificationSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);
    private static final String QR_API_BASE = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=";
    private static final Duration QR_FETCH_TIMEOUT = Duration.ofSeconds(3);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final HttpClient httpClient;

    public EmailNotificationSender(
            JavaMailSender mailSender,
            @Value("${notification.delivery.from-address}") String fromAddress,
            @Value("${notification.delivery.from-name:LoraFilm}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(QR_FETCH_TIMEOUT)
                .build();
    }

    @Override
    public Channel supportedChannel() {
        return Channel.EMAIL;
    }

    @Override
    public DeliveryResult send(RenderedNotification notification) {
        if (notification.destination() == null || !notification.destination()
                .matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return DeliveryResult.failure("smtp", FailureCategory.INVALID_RECIPIENT,
                    "INVALID_EMAIL", "Email destination is invalid", null);
        }
        try {
            String finalHtml = notification.htmlContent();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.destination());
            helper.setSubject(notification.subject());
            helper.setText(notification.textContent(), finalHtml);
            message.setHeader("X-LoraFilm-Notification-Id", notification.notificationPublicId());

            mailSender.send(message);
            String providerId = message.getMessageID();
            return DeliveryResult.success("smtp",
                    providerId == null ? "smtp-" + UUID.randomUUID() : providerId);
        } catch (MailAuthenticationException exception) {
            return DeliveryResult.failure("smtp", FailureCategory.AUTHENTICATION_ERROR,
                    "SMTP_AUTHENTICATION_FAILED", "Email provider authentication failed", null);
        } catch (MailSendException exception) {
            return DeliveryResult.failure("smtp", FailureCategory.TRANSIENT,
                    "SMTP_SEND_FAILED", "Email provider is temporarily unavailable", null);
        } catch (Exception exception) {
            return DeliveryResult.failure("smtp", FailureCategory.PROVIDER_REJECTED,
                    "SMTP_REJECTED", "Email provider rejected the message", null);
        }
    }

    /**
     * Fetches the QR image for each ticket and injects a styled HTML section
     * (using {@code data:image/png;base64,...} URIs) before the {@code </body>} tag.
     * Works without any template changes.
     *
     * @param htmlContent original rendered HTML from the template engine
     * @param payload     full notification payload containing the {@code tickets} list
     * @return HTML string with QR section appended inside the body
     */
    private String injectQrSection(String htmlContent, Map<String, Object> payload) {
        if (payload == null || htmlContent == null) return htmlContent;

        List<TicketQr> ticketQrs = buildTicketQrList(payload);
        if (ticketQrs.isEmpty()) return htmlContent;

        String qrBlock = buildQrHtmlBlock(ticketQrs);

        // Insert before </body>; fall back to appending if tag is absent
        int bodyCloseIdx = htmlContent.toLowerCase().lastIndexOf("</body>");
        if (bodyCloseIdx >= 0) {
            return htmlContent.substring(0, bodyCloseIdx) + qrBlock + htmlContent.substring(bodyCloseIdx);
        }
        return htmlContent + qrBlock;
    }

    /** Resolves each ticket's code, fetches the QR PNG (or falls back to direct URL). */
    private List<TicketQr> buildTicketQrList(Map<String, Object> payload) {
        List<Map<?, ?>> tickets = extractTickets(payload);
        List<TicketQr> result = new ArrayList<>();

        if (tickets.isEmpty()) {
            // Legacy fallback: single-ticket booking
            String code = resolveFirstTicketCode(payload);
            if (code != null) {
                String b64 = fetchQrAsBase64(code);
                String url = QR_API_BASE + java.net.URLEncoder.encode(code, StandardCharsets.UTF_8);
                result.add(new TicketQr(code, b64, url, null, null));
            }
            return result;
        }

        for (Map<?, ?> ticket : tickets) {
            String code = resolveTicketCode(ticket);
            if (code == null) continue;
            String b64 = fetchQrAsBase64(code);
            String url = QR_API_BASE + java.net.URLEncoder.encode(code, StandardCharsets.UTF_8);
            String seat = objectStr(ticket.get("seatLabel"));
            String type = objectStr(ticket.get("seatType"));
            result.add(new TicketQr(code, b64, url, seat, type));
        }
        return result;
    }

    /** Builds a styled, self-contained HTML block with one QR card per ticket. */
    private String buildQrHtmlBlock(List<TicketQr> ticketQrs) {
        StringBuilder sb = new StringBuilder();
        sb.append(
            "<div style=\"font-family:Arial,sans-serif;margin:32px auto;max-width:600px;padding:0 16px;\">\n" +
            "  <hr style=\"border:none;border-top:1px solid #e5e7eb;margin-bottom:24px;\"/>\n" +
            "  <p style=\"font-size:11px;font-weight:700;text-transform:uppercase;" +
            "letter-spacing:0.1em;color:#6b7280;margin:0 0 16px;\">" +
            "M&#195;&#163; QR V&#192;O PH&#210;NG CHI&#7870;U</p>\n" +
            "  <table cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">" +
            "<tr valign=\"top\">\n"
        );

        for (TicketQr tq : ticketQrs) {
            String label = (tq.seat() != null && tq.type() != null)
                    ? "Gh&#7871; " + escapeHtml(tq.seat()) + " &middot; " + escapeHtml(tq.type())
                    : escapeHtml(tq.code());
            String imgSrc = tq.base64() != null
                    ? "data:image/png;base64," + tq.base64()
                    : tq.imageUrl();
            sb.append(
                "<td style=\"padding:0 12px 0 0;\">" +
                "<div style=\"background:#f9fafb;border:1px solid #e5e7eb;border-radius:12px;" +
                "padding:16px;text-align:center;width:160px;\">" +
                "<img src=\"" + imgSrc + "\"" +
                " alt=\"QR " + label + "\"" +
                " width=\"160\" height=\"160\"" +
                " style=\"display:block;margin:0 auto 8px;border-radius:6px;\"/>" +
                "<p style=\"font-size:10px;color:#374151;font-weight:600;margin:0;\">" + label + "</p>" +
                "</div>" +
                "</td>\n"
            );
        }

        sb.append("</tr></table>\n</div>\n");
        return sb.toString();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Fetches a QR image and returns it as a Base64 string, or {@code null} on failure. */
    private String fetchQrAsBase64(String qrData) {
        try {
            String encoded = java.net.URLEncoder.encode(qrData, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(QR_API_BASE + encoded))
                    .timeout(QR_FETCH_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200 && resp.body() != null && resp.body().length > 0) {
                return Base64.getEncoder().encodeToString(resp.body());
            }
            log.warn("QR API returned status={} for qrData={}", resp.statusCode(), qrData);
        } catch (Exception ex) {
            log.warn("Failed to fetch QR image for qrData={}: {}", qrData, ex.getMessage());
        }
        return null;
    }

    private List<Map<?, ?>> extractTickets(Map<String, Object> payload) {
        Object obj = payload.get("tickets");
        if (!(obj instanceof List<?> list)) return List.of();
        List<Map<?, ?>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) result.add(m);
        }
        return result;
    }

    private String resolveTicketCode(Map<?, ?> ticket) {
        Object code = ticket.get("ticketCode");
        if (code instanceof String s && !s.isBlank()) return s;
        Object url = ticket.get("ticketAccessUrl");
        if (url instanceof String s && !s.isBlank()) return s;
        return null;
    }

    private String resolveFirstTicketCode(Map<String, Object> payload) {
        Object code = payload.get("ticketCode");
        if (code instanceof String s && !s.isBlank()) return s;
        Object url = payload.get("ticketAccessUrl");
        if (url instanceof String s && !s.isBlank()) return s;
        Object codes = payload.get("ticketCodes");
        if (codes instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    private String objectStr(Object value) {
        return (value instanceof String s && !s.isBlank()) ? s : null;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Holds resolved data for a single ticket's QR image. */
    private record TicketQr(String code, String base64, String imageUrl, String seat, String type) {}
}
