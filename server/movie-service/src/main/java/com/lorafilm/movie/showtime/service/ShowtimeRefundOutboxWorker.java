package com.lorafilm.movie.showtime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ShowtimeRefundOutboxWorker {
    private static final Logger log =
            LoggerFactory.getLogger(ShowtimeRefundOutboxWorker.class);

    private final ShowtimeRefundOutboxService service;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String paymentBaseUrl;
    private final String internalToken;
    private final Duration readTimeout;

    public ShowtimeRefundOutboxWorker(
            ShowtimeRefundOutboxService service,
            ObjectMapper objectMapper,
            @Value("${payment.service.base-url:http://localhost:8084}") String paymentBaseUrl,
            @Value("${payment.service.internal-token:}") String internalToken,
            @Value("${payment.service.connect-timeout-millis:5000}") int connectTimeout,
            @Value("${payment.service.read-timeout-millis:10000}") int readTimeout) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.paymentBaseUrl = paymentBaseUrl.replaceAll("/+$", "");
        this.internalToken = internalToken;
        this.readTimeout = Duration.ofMillis(readTimeout);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
    }

    @Scheduled(
            fixedDelayString = "${payment.refund-outbox.fixed-delay-millis:3000}",
            initialDelayString = "${payment.refund-outbox.fixed-delay-millis:3000}")
    public void deliver() {
        if (internalToken == null || internalToken.isBlank()) {
            return;
        }
        String ownerToken = "movie-refund-outbox-" + UUID.randomUUID();
        for (Long id : service.claim(ownerToken)) {
            try {
                ShowtimeRefundOutboxService.WorkItem item =
                        service.work(id, ownerToken);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("eventId", item.eventId());
                body.put("note", item.cancellationReason());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(paymentBaseUrl
                                + "/internal/payments/refunds/showtimes/"
                                + item.showtimePublicId()))
                        .timeout(readTimeout)
                        .header("Content-Type", "application/json")
                        .header("X-Internal-Token", internalToken)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(body)))
                        .build();
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException(
                            "Payment Service trả về HTTP " + response.statusCode());
                }
                service.published(id, ownerToken);
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Cannot deliver showtime refund trigger: id={}, error={}",
                        id, rootMessage(exception));
                service.failed(id, ownerToken, rootMessage(exception));
            }
        }
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null
                ? current.getClass().getSimpleName() : current.getMessage();
    }
}
