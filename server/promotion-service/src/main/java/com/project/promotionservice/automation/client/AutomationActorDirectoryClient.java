package com.project.promotionservice.automation.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Best-effort actor resolver for operational read models. Actor identifiers
 * remain in the technical payload, while the main UI receives a stable,
 * human-readable label and never fails just because User Service is down.
 */
@Component
public class AutomationActorDirectoryClient {
    private final RestClient restClient;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public AutomationActorDirectoryClient(
            RestClient.Builder builder,
            @Value("${promotion.user-service.url:http://localhost:8086}") String serviceUrl,
            @Value("${promotion.user-service.internal-token:${USER_SERVICE_INTERNAL_TOKEN:}}")
            String internalToken) {
        this.restClient = builder.baseUrl(serviceUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    public String displayName(String actorId) {
        if (actorId == null || actorId.isBlank()) return null;
        if ("SYSTEM".equalsIgnoreCase(actorId)) return "Hệ thống tự động";
        if (!actorId.matches("[1-9][0-9]{0,18}")) return "Tài khoản nội bộ";
        return cache.computeIfAbsent(actorId, this::resolve);
    }

    private String resolve(String actorId) {
        try {
            JsonNode envelope = restClient.get()
                    .uri("/api/v1/internal/users/{accountId}/notification-recipient",
                            Long.valueOf(actorId))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = envelope == null ? null : envelope.path("data");
            String fullName = text(data, "fullName");
            if (fullName != null) return fullName;
            String email = text(data, "email");
            return email == null ? "Tài khoản nội bộ" : email;
        } catch (RestClientException | NumberFormatException exception) {
            return "Tài khoản nội bộ";
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.path(field).asText("").trim();
        return value.isBlank() ? null : value;
    }
}
